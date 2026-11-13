package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.FirebaseMessaging;
import com.projectx.notification.sender.ChannelNotConfiguredException;
import com.projectx.notification.sender.FirebaseProperties;
import com.projectx.notification.sender.FirebasePushSender;
import com.projectx.notification.service.DeviceTokenService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Plain unit test (no Spring context needed) for production-hardening part 3 - see
 * PLAN-production-hardening.md section 3. There are no real Firebase credentials anywhere this
 * runs, so "enabled" is exercised with a mocked FirebaseMessaging client via the class's test-only
 * seam (setMessagingClientForTests) instead of real initialization/network calls.
 */
class FirebasePushSenderTest {

    @Test
    void disabledByDefaultIsASafeNoOpExactlyLikeTheOldNoOpPushSender() {
        FirebaseProperties disabled = new FirebaseProperties();
        disabled.setEnabled(false);
        DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
        FirebasePushSender sender = new FirebasePushSender(disabled, deviceTokenService);

        assertThatThrownBy(() -> sender.send(1L, "title", "body"))
                .isInstanceOf(ChannelNotConfiguredException.class);
        // Never even attempted to look up a device token - nothing happened besides the no-op throw.
        verify(deviceTokenService, org.mockito.Mockito.never()).findToken(any());
    }

    @Test
    void enabledWithAStoredDeviceTokenAttemptsToSendViaFirebase() throws Exception {
        FirebaseProperties enabled = new FirebaseProperties();
        enabled.setEnabled(true);
        DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
        when(deviceTokenService.findToken(42L)).thenReturn(Optional.of("device-token-abc"));
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        when(mockMessaging.send(any())).thenReturn("projects/p/messages/1");

        FirebasePushSender sender = new FirebasePushSender(enabled, deviceTokenService);
        sender.setMessagingClientForTests(mockMessaging);

        sender.send(42L, "Driving license expiring soon", "body text");

        verify(mockMessaging).send(any());
    }

    @Test
    void enabledButNoStoredDeviceTokenIsSkippedNotConfiguredNotAFailure() {
        FirebaseProperties enabled = new FirebaseProperties();
        enabled.setEnabled(true);
        DeviceTokenService deviceTokenService = mock(DeviceTokenService.class);
        when(deviceTokenService.findToken(99L)).thenReturn(Optional.empty());
        FirebasePushSender sender = new FirebasePushSender(enabled, deviceTokenService);
        sender.setMessagingClientForTests(mock(FirebaseMessaging.class));

        assertThatThrownBy(() -> sender.send(99L, "title", "body"))
                .isInstanceOf(ChannelNotConfiguredException.class)
                .hasMessageContaining("99");
    }
}
