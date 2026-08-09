package com.projectx.notification.sender;

import org.springframework.stereotype.Component;

@Component
public class NoOpPushSender implements PushSender {

    @Override
    public void send(Long recipientUserId, String title, String body) {
        throw new ChannelNotConfiguredException("Push notifications not wired yet (no FCM credentials configured)");
    }
}
