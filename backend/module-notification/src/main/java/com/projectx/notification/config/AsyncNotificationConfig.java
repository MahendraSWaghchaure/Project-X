package com.projectx.notification.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A bounded worker pool for notification dispatch, so one slow SMS/email provider call never
 * blocks the request thread that raised the event (geofence crossing, SOS trigger, etc). This is
 * the in-process seam a real queue (SQS/Kafka consumer) occupies once this needs to run on more
 * than one instance - see backend/ARCHITECTURE.md.
 */
@Configuration
@EnableAsync
public class AsyncNotificationConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "notificationExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notify-");
        executor.initialize();
        return executor;
    }
}
