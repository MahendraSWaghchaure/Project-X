package com.projectx.tracking.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Every live-broadcast (vehicle position, SOS) goes through Redis pub/sub instead of straight to
 * SimpMessagingTemplate, so it reaches every app instance's locally-connected STOMP sessions, not
 * just whichever instance happened to ingest the ping. Today there's one instance, so this is a
 * no-op round trip through Redis - but the code is already correct for N instances, which is the
 * actual bottleneck TrackingWebSocketConfig's in-memory SimpleBrokerMessageHandler had before this
 * (see backend/ARCHITECTURE.md).
 */
@Component
public class StompRelayPublisher {

    private static final Logger log = LoggerFactory.getLogger(StompRelayPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public StompRelayPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String destination, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String envelope = objectMapper.writeValueAsString(new StompRelayMessage(destination, payloadJson));
            redisTemplate.convertAndSend(StompRelayConfig.CHANNEL, envelope);
        } catch (Exception e) {
            log.warn("Could not publish STOMP relay message for {}", destination, e);
        }
    }
}
