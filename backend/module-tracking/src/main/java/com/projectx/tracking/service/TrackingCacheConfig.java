package com.projectx.tracking.service;

import java.time.Duration;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

/**
 * School location and route-stop geometry change essentially never (a school moves, a stop gets
 * repositioned) but are read on every single ingested GPS ping via GeofenceEvaluationService -
 * the busiest read path in the whole app. Cached for 10 minutes: long enough to matter at real
 * ping volume, short enough that an admin editing a school's location doesn't leave stale data
 * around for an inconvenient length of time (no explicit @CacheEvict wiring in this pass - see
 * GeoLookupCacheService).
 */
@Configuration
@EnableCaching
public class TrackingCacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer geoLookupCacheCustomizer() {
        return builder -> builder.withCacheConfiguration("schoolGeoInfo",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("routeStopGeoInfo",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)));
    }
}
