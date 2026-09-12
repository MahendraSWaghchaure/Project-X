package com.projectx.tracking.service;

import com.projectx.common.trace.MdcTaskDecorator;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A bounded worker pool for the geofence/safety/cache/broadcast work behind every ingested ping -
 * separate from module-notification's pool since GPS ingestion and notification delivery have very
 * different volume/latency characteristics and shouldn't compete for the same threads.
 */
@Configuration
@EnableAsync
public class AsyncTrackingConfig {

    @Bean(name = "trackingExecutor")
    public Executor trackingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("tracking-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}
