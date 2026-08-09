package com.projectx.notification.sender;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * JavaMailSender only auto-configures when spring.mail.host is set, so it's injected via
 * ObjectProvider - lets the app boot fine with email left unconfigured in local/dev.
 */
@Component
@EnableConfigurationProperties(EmailProperties.class)
public class SpringEmailSender implements EmailSender {

    private final EmailProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public SpringEmailSender(EmailProperties properties, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.properties = properties;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void send(String toEmail, String subject, String body) {
        if (!properties.isEnabled()) {
            throw new ChannelNotConfiguredException("Email not configured");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new ChannelNotConfiguredException("Email enabled but no spring.mail.host configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(properties.getFromAddress());
        mailSender.send(message);
    }
}
