package com.projectx.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A crude but effective brute-force/abuse guard on the handful of endpoints reachable with no
 * token at all: login (credential stuffing), refresh, and public driver self-registration (fake
 * account spam). Counters live in Redis (already a dependency for location caching) rather than an
 * in-process map, so the limit holds even if this ever runs as more than one instance.
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

    private final Map<String, Integer> limitsPerWindow;
    private final Duration window = Duration.ofMinutes(1);
    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.limitsPerWindow = Map.of(
                "/auth/login", properties.getLoginPerMinute(),
                "/auth/refresh", properties.getRefreshPerMinute(),
                "/drivers/register", properties.getDriverRegisterPerMinute(),
                "/auth/forgot-password", properties.getForgotPasswordPerMinute()
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Integer limit = limitsPerWindow.get(request.getRequestURI());
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = "ratelimit:%s:%s".formatted(request.getRequestURI(), clientIp(request));
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }

        if (count != null && count > limit) {
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

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
    }
}
