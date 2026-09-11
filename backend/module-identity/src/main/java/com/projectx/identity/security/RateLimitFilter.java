package com.projectx.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private final List<LimitRule> rules;
    private final Duration window = Duration.ofMinutes(1);
    private final StringRedisTemplate redisTemplate;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.rules = List.of(
                new LimitRule("/auth/login", properties.getLoginPerMinute()),
                new LimitRule("/auth/refresh", properties.getRefreshPerMinute()),
                new LimitRule("/drivers/register", properties.getDriverRegisterPerMinute()),
                new LimitRule("/auth/forgot-password", properties.getForgotPasswordPerMinute()),
                new LimitRule("/trips/*/sos", properties.getSosTriggerPerMinute()),
                new LimitRule("/schools/*/children", properties.getChildCreatePerMinute()),
                new LimitRule("/parents/me/children", properties.getChildCreatePerMinute()),
                new LimitRule("/parents/me/payments/*orders", properties.getPaymentOrderPerMinute())
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
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }

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

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }
}
