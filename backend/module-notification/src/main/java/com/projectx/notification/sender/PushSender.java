package com.projectx.notification.sender;

/** No real implementation yet - wire a Firebase Cloud Messaging sender here once a service-account credential exists. See NoOpPushSender. */
public interface PushSender {
    void send(Long recipientUserId, String title, String body);
}
