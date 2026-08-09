package com.projectx.notification.sender;

public interface SmsSender {
    void send(String toPhoneE164, String message);
}
