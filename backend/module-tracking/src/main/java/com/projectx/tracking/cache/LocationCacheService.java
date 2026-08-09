package com.projectx.tracking.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class LocationCacheService {

    private static final Logger log = LoggerFactory.getLogger(LocationCacheService.class);
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public LocationCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void put(CachedLocation location) {
        try {
            redisTemplate.opsForValue().set(key(location.vehicleId()), objectMapper.writeValueAsString(location), TTL);
        } catch (JsonProcessingException e) {
            log.warn("Could not cache location for vehicle {}", location.vehicleId(), e);
        }
    }

    public Optional<CachedLocation> get(Long vehicleId) {
        String json = redisTemplate.opsForValue().get(key(vehicleId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CachedLocation.class));
        } catch (JsonProcessingException e) {
            log.warn("Could not read cached location for vehicle {}", vehicleId, e);
            return Optional.empty();
        }
    }

    private String key(Long vehicleId) {
        return "vehicle:location:" + vehicleId;
    }
}
