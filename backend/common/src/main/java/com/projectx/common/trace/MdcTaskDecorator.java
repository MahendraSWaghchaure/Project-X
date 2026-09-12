package com.projectx.common.trace;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * MDC is thread-bound and Spring's @Async executors run submitted work on a different thread than
 * the one that submitted it, so without this, every log line written inside notificationExecutor/
 * trackingExecutor work (see AsyncNotificationConfig/AsyncTrackingConfig) would carry no trace id
 * at all - exactly the async work (SMS/email dispatch, geofence evaluation) that's hardest to
 * correlate back to the request that caused it otherwise. Captures the submitting thread's MDC
 * snapshot at submission time and restores it on the worker thread for the duration of the task,
 * then restores whatever the worker thread had before (usually nothing, but never assume) - pooled
 * threads must not leak one task's context into the next.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
