package com.projectx.tracking.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.ChannelTopic;

@Configuration
public class StompRelayConfig {

    static final String CHANNEL = "projectx:stomp-relay";

    @Bean
    public RedisMessageListenerContainer stompRelayListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       StompRelaySubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(CHANNEL));
        return container;
    }
}
