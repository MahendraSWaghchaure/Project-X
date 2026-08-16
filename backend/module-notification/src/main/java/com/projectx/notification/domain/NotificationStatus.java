package com.projectx.notification.domain;

public enum NotificationStatus {
    SENT,
    SKIPPED_NOT_CONFIGURED,
    FAILED,
    /** Every retry attempt (see RetryingChannelDispatcher) was exhausted - a human needs to look at this one. */
    DEAD_LETTERED
}
