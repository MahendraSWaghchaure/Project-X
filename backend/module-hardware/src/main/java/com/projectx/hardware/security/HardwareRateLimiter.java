package com.projectx.hardware.security;

import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Per-device-id rate limiting for the hardware ingestion endpoints, backed by Redis so the limit
 * holds across more than one app instance - same atomic INCR+PEXPIRE Lua script as
 * module-identity's RateLimitFilter (see that class's javadoc for why a single non-atomic
 * INCR-then-EXPIRE is unsafe), reimplemented here rather than reused directly since that filter is
 * IP-keyed and wired into the JWT-auth SecurityFilterChain - this is deviceId-keyed and runs from
 * a plain MVC interceptor instead (see DeviceApiKeyAuthInterceptor).
 */
@Component
public class HardwareRateLimiter {

    private static final RedisScript<Long> INCR_WITH_EXPIRE = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    public HardwareRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** True if this request is within the per-minute cap for this (bucket, deviceId) pair. */
    public boolean allow(String bucket, String deviceId, int limitPerMinute) {
        String key = "hardware-ratelimit:%s:%s".formatted(bucket, deviceId);
        Long count = redisTemplate.execute(INCR_WITH_EXPIRE, List.of(key), String.valueOf(WINDOW.toMillis()));
        return count == null || count <= limitPerMinute;
    }
}
