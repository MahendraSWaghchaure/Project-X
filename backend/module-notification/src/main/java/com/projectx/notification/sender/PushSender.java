package com.projectx.notification.sender;

/** See {@link FirebasePushSender} - the sole implementation, disabled (behaves as a no-op) until real Firebase credentials are configured. */
public interface PushSender {
    void send(Long recipientUserId, String title, String body);
}
