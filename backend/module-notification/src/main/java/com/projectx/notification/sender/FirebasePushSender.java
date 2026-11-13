package com.projectx.notification.sender;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.projectx.notification.service.DeviceTokenService;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Real push sender, following the exact "disabled until configured" pattern TwilioSmsSender
 * already uses: a boolean {@code enabled} property (default false, same as Twilio/SMTP) plus a
 * credentials property ({@code serviceAccountJsonPath}); when disabled, {@link #send} always
 * throws {@link ChannelNotConfiguredException} - byte-for-byte the same observable behavior the
 * previous NoOpPushSender had, so there is NO behavioral change with the defaults (the only way
 * this can be verified right now - there are no real Firebase credentials on this machine or in
 * any test environment). Replaces NoOpPushSender as the sole {@code PushSender} bean (two
 * {@code @Component}s implementing the same single-method interface would make Spring's
 * constructor injection into NotificationDispatchService ambiguous, so there is deliberately only
 * ever one).
 */
@Component
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebasePushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushSender.class);

    private final FirebaseProperties properties;
    private final DeviceTokenService deviceTokenService;
    private FirebaseMessaging messaging;

    public FirebasePushSender(FirebaseProperties properties, DeviceTokenService deviceTokenService) {
        this.properties = properties;
        this.deviceTokenService = deviceTokenService;
    }

    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            log.info("Push notifications disabled (set projectx.notification.push.enabled=true with a Firebase "
                    + "service-account-json-path to enable)");
            return;
        }
        try (FileInputStream credentials = new FileInputStream(properties.getServiceAccountJsonPath())) {
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(credentials)).build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            this.messaging = FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            log.error("Could not initialize Firebase from projectx.notification.push.service-account-json-path={} - "
                    + "push notifications will fail (SKIPPED_NOT_CONFIGURED) until this is fixed",
                    properties.getServiceAccountJsonPath(), e);
        }
    }

    @Override
    public void send(Long recipientUserId, String title, String body) {
        if (!properties.isEnabled() || messaging == null) {
            throw new ChannelNotConfiguredException("Push not configured");
        }
        String token = deviceTokenService.findToken(recipientUserId)
                .orElseThrow(() -> new ChannelNotConfiguredException("No device token registered for user " + recipientUserId));
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();
        try {
            messaging.send(message);
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException("Firebase push send failed", e);
        }
    }

    /**
     * Test seam only: lets a test substitute a mocked {@link FirebaseMessaging} client without real
     * Firebase credentials (none exist on this machine) or a network call, so "attempts to send when
     * enabled" can be verified. Never called by production code - {@link #init()} is the only other
     * writer of this field.
     */
    public void setMessagingClientForTests(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }
}
