package com.projectx.tracking.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/** Runs on every instance; re-broadcasts whatever any instance published to this instance's own locally-connected STOMP sessions. */
@Component
public class StompRelaySubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(StompRelaySubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public StompRelaySubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            StompRelayMessage relayMessage = objectMapper.readValue(message.getBody(), StompRelayMessage.class);
            messagingTemplate.send(relayMessage.destination(), MessageBuilder.withPayload(relayMessage.payloadJson())
                    .setHeader("contentType", MimeTypeUtils.APPLICATION_JSON)
                    .build());
        } catch (Exception e) {
            log.warn("Could not relay STOMP message from Redis pub/sub", e);
        }
    }
}
