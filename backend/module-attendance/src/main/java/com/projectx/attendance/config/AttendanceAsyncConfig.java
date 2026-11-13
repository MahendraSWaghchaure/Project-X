package com.projectx.attendance.config;

import com.projectx.common.trace.MdcTaskDecorator;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded pool for the arrival pipeline (driver event -> Arrival row -> parent/school/trip STOMP
 * push + notifications). Deliberately separate from trackingExecutor/notificationExecutor so a
 * burst of GPS pings or a slow SMS provider can't starve safe-arrival processing, and vice versa.
 *
 * <p>MdcTaskDecorator carries the originating request's X-Request-Id onto the worker thread, so
 * the whole chain is greppable by one trace id. CallerRunsPolicy = back-pressure instead of
 * dropping: if the 2000-deep queue ever fills, the submitting request thread does the work itself
 * (slower response, nothing lost), which is the right failure mode for a safety feature.
 */
@Configuration
@EnableAsync
public class AttendanceAsyncConfig {

    @Bean(name = "attendanceExecutor")
    public Executor attendanceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("attendance-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
