package com.projectx.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A crude but effective brute-force/abuse guard, backed by Redis (already a dependency for
 * location caching) so the limit holds across more than one app instance. Covers both the
 * endpoints reachable with no token at all (login/refresh/forgot-password/driver self-register -
 * credential stuffing, fake accounts) and a handful of authenticated business endpoints worth
 * throttling regardless of who's calling them (SOS trigger, child creation, payment order
 * creation) - a compromised or scripted account hammering these is still real abuse even though
 * it's authenticated.
 *
 * <p>Patterns are matched with {@link AntPathMatcher} rather than an exact-URI map so
 * path-variable endpoints (e.g. {@code /trips/{tripId}/sos}) can be covered.
 *
 * <p>Keyed by IP, not by authenticated user: this filter runs (see SecurityConfig's
 * {@code addFilterAfter(rateLimitFilter, CorsFilter.class)}) before the OAuth2 resource-server
 * filter that actually validates the JWT and populates the SecurityContext, so there is no
 * authenticated principal available here to key on without re-parsing the token ourselves - not
 * worth the duplicated, easy-to-get-subtly-wrong logic for a rate limiter whose job is picking a
 * fair bucket, not authenticating. IP-based still meaningfully throttles a single abusive
 * client/script regardless of which (or how many) accounts' tokens it cycles through.
 *
 * <p>Deliberately NOT a {@code @Component} - Spring Boot auto-registers any Filter bean as a
 * global servlet filter, which would run this a second time outside SecurityConfig's explicit
 * placement below and double-count every request against Redis. It's wired in as a plain
 * {@code @Bean} inside SecurityConfig instead, positioned after CORS so a 429 still carries the
 * Access-Control-Allow-Origin header the browser needs to let the frontend read the response.
 *
 * <p>Limits are constructor-injected (see RateLimitProperties) rather than hardcoded so the
 * integration test suite - which legitimately logs in far more than 20 times a minute across its
 * whole run - can raise them instead of fighting production-appropriate defaults.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private record LimitRule(String pattern, int limitPerMinute) {}

    /**
     * Atomic INCR-then-conditionally-EXPIRE as a single Lua script. Two separate calls (the
     * previous implementation) can't be made atomic from the client side: if the process crashes,
     * the connection drops, or Redis itself fails over between the INCR and the EXPIRE, the key is
     * left with no TTL and the IP is rate-limited forever until someone notices and deletes it by
     * hand (this happened once during testing). A Lua script runs as a single operation on the
     * Redis server, so there's no window for a crash to land between the two steps.
     */
    private static final RedisScript<Long> INCR_WITH_EXPIRE = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final List<LimitRule> rules;
    private final Duration window = Duration.ofMinutes(1);
    private final StringRedisTemplate redisTemplate;
    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final Set<String> trustedProxies;

    public RateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.trustedProxies = Set.copyOf(properties.getTrustedProxies());
        this.rules = List.of(
                new LimitRule("/auth/login", properties.getLoginPerMinute()),
                new LimitRule("/auth/refresh", properties.getRefreshPerMinute()),
                new LimitRule("/drivers/register", properties.getDriverRegisterPerMinute()),
                new LimitRule("/auth/forgot-password", properties.getForgotPasswordPerMinute()),
                new LimitRule("/trips/*/sos", properties.getSosTriggerPerMinute()),
                new LimitRule("/schools/*/children", properties.getChildCreatePerMinute()),
                new LimitRule("/parents/me/children", properties.getChildCreatePerMinute()),
                new LimitRule("/parents/me/payments/*orders", properties.getPaymentOrderPerMinute()),
                new LimitRule("/schools/*/attendance/bulk", properties.getAttendanceBulkPerMinute()),
                new LimitRule("/schools/*/students/import", properties.getStudentImportPerMinute()),
                new LimitRule("/parents/me/arrivals/*/respond", properties.getArrivalRespondPerMinute()),
                new LimitRule("/public/consent/*", properties.getConsentViewPerMinute()),
                new LimitRule("/public/consent/*/respond", properties.getConsentRespondPerMinute()),
                new LimitRule("/public/inquiries", properties.getInquiryPerMinute()),
                new LimitRule("/schools/*/students/*/consent-link", properties.getConsentLinkPerMinute())
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LimitRule rule = matchRule(request.getRequestURI());
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = "ratelimit:%s:%s".formatted(rule.pattern(), clientIp(request));
        Long count = redisTemplate.execute(INCR_WITH_EXPIRE, List.of(key), String.valueOf(window.toMillis()));

        if (count != null && count > rule.limitPerMinute()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"success":false,"data":null,"error":{"code":"RATE_LIMITED","message":\
                    "Too many requests - please wait a minute and try again.","fieldErrors":[]},\
                    "timestamp":"%s"}""".formatted(Instant.now()));
            return;
        }

        chain.doFilter(request, response);
    }

    private LimitRule matchRule(String requestUri) {
        for (LimitRule rule : rules) {
            if (pathMatcher.match(rule.pattern(), requestUri)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * X-Forwarded-For is set by the CLIENT in the raw request, same as any other header - trusting
     * it unconditionally means any caller can put a fresh random value on every request and reset
     * their own bucket every time, which a tester proved defeats this limiter entirely (confirmed:
     * 600+ bulk calls/minute all returned 200 with a spoofed value, vs. 429 on the 61st with a fixed
     * one). It is only trustworthy when it was actually ADDED by a reverse proxy we control sitting
     * between the client and us - i.e. only when the TCP connection reaching this server came from
     * that proxy's own address, not the client's. If remoteAddr isn't a configured trusted proxy,
     * the header is attacker-controlled and ignored outright.
     */
    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor.split(",")[0].trim() : remoteAddr;
    }
}
