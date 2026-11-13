package com.projectx.attendance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectx.attendance.dto.AttendanceDtos.AttendanceSummary;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Short-TTL Redis cache for the "today's roster" summary - the single hottest read (every school
 * admin dashboard polls it). The TTL is the staleness bound; writes (bulk attendance, arrival
 * changes) also evict so the common case is fresher than the TTL. Redis being down degrades to
 * a direct (still cheap, single-statement aggregate) DB read - the cache is never load-bearing.
 */
@Component
public class SummaryCache {

    private static final Logger log = LoggerFactory.getLogger(SummaryCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public SummaryCache(StringRedisTemplate redis, ObjectMapper objectMapper,
                        @Value("${projectx.attendance.summary-cache-ttl-seconds:15}") long ttlSeconds) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Optional<AttendanceSummary> get(Long schoolId, LocalDate date) {
        try {
            String json = redis.opsForValue().get(key(schoolId, date));
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, AttendanceSummary.class));
        } catch (Exception e) {
            log.warn("Attendance summary cache read failed - falling back to DB", e);
            return Optional.empty();
        }
    }

    public void put(Long schoolId, LocalDate date, AttendanceSummary summary) {
        try {
            redis.opsForValue().set(key(schoolId, date), objectMapper.writeValueAsString(summary), ttl);
        } catch (Exception e) {
            log.warn("Attendance summary cache write failed", e);
        }
    }

    public void evict(Long schoolId, LocalDate date) {
        try {
            redis.delete(key(schoolId, date));
        } catch (Exception e) {
            log.warn("Attendance summary cache evict failed (entry expires within {}s anyway)", ttl.toSeconds(), e);
        }
    }

    private static String key(Long schoolId, LocalDate date) {
        return "attendance:summary:" + schoolId + ":" + date;
    }
}
