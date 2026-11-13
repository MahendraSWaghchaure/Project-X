package com.projectx.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One active FCM device token per user - see FirebasePushSender and DeviceTokenService. */
@Entity
@Table(name = "push_device_tokens")
public class PushDeviceToken {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "device_token", nullable = false, length = 512)
    private String deviceToken;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PushDeviceToken() {
    }

    public PushDeviceToken(Long userId, String deviceToken) {
        this.userId = userId;
        this.deviceToken = deviceToken;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateToken(String deviceToken) {
        this.deviceToken = deviceToken;
        this.updatedAt = Instant.now();
    }
}
