package com.projectx.notification.sender;

public interface EmailSender {
    void send(String toEmail, String subject, String body);
}
