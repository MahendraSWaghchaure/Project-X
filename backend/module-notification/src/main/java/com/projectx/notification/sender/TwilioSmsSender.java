package com.projectx.notification.sender;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(TwilioProperties.class)
public class TwilioSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    private final TwilioProperties properties;

    public TwilioSmsSender(TwilioProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (properties.isEnabled()) {
            Twilio.init(properties.getAccountSid(), properties.getAuthToken());
        } else {
            log.info("SMS notifications disabled (set projectx.notification.sms.enabled=true with Twilio creds to enable)");
        }
    }

    @Override
    public void send(String toPhoneE164, String message) {
        if (!properties.isEnabled()) {
            throw new ChannelNotConfiguredException("SMS not configured");
        }
        Message.creator(new PhoneNumber(toPhoneE164), new PhoneNumber(properties.getFromNumber()), message).create();
    }
}
