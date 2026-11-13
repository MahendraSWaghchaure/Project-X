package com.projectx.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Every alert this system sends (or tries to) is logged here - the audit trail the product's whole "accountability" pitch rests on. */
@Entity
@Table(name = "notification_records")
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for messages to a phone number that is not a user account (see recipientContact). */
    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    /** Masked phone/email for external recipients, e.g. "******3210". */
    @Column(name = "recipient_contact", length = 64)
    private String recipientContact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(nullable = false, length = 64)
    private String templateKey;

    @Column(nullable = false, length = 1000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected NotificationRecord() {
    }

    public NotificationRecord(Long recipientUserId, NotificationChannel channel, String templateKey, String body,
                               NotificationStatus status, String failureReason) {
        this.recipientUserId = recipientUserId;
        this.channel = channel;
        this.templateKey = templateKey;
        this.body = body;
        this.status = status;
        this.failureReason = failureReason;
    }

    public Long getId() {
        return id;
    }

    public String getRecipientContact() {
        return recipientContact;
    }

    public static NotificationRecord external(String maskedContact, NotificationChannel channel, String templateKey, String body,
                                              NotificationStatus status, String failureReason) {
        NotificationRecord record = new NotificationRecord(null, channel, templateKey, body, status, failureReason);
        record.recipientContact = maskedContact;
        return record;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
