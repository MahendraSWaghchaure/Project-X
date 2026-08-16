package com.projectx.notification.service;

import com.projectx.notification.sender.ChannelNotConfiguredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Retries a single channel send up to 3 times with exponential backoff before giving up - covers
 * the common case of a transient network blip to Twilio/SMTP. A misconfigured channel
 * (ChannelNotConfiguredException) is never retried since retrying can't fix a missing API key.
 * A plain loop rather than an annotation-driven retry library: this already runs on the dedicated
 * notificationExecutor pool (see AsyncNotificationConfig), so blocking briefly between attempts
 * costs nothing a request thread would ever feel, and it avoids pulling in an AOP-proxy dependency
 * for three lines of actual logic.
 */
@Component
public class RetryingChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(RetryingChannelDispatcher.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 500;

    public void send(Runnable action) {
        long backoffMs = INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                action.run();
                return;
            } catch (ChannelNotConfiguredException e) {
                throw e;
            } catch (RuntimeException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("Notification send attempt {}/{} failed, retrying in {}ms: {}", attempt, MAX_ATTEMPTS, backoffMs, e.getMessage());
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry notification send", e);
        }
    }
}
