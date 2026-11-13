package com.projectx.notification.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A "contact us" message from the public site footer. {@code contact} is whatever the visitor typed (a phone number or an email). */
@Entity
@Table(name = "inquiries")
public class Inquiry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String contact;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(name = "handled_by_user_id")
    private Long handledByUserId;

    @Column(name = "handled_at")
    private Instant handledAt;

    protected Inquiry() {
    }

    public Inquiry(String name, String contact, String message) {
        this.name = name;
        this.contact = contact;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContact() {
        return contact;
    }

    public String getMessage() {
        return message;
    }

    public InquiryStatus getStatus() {
        return status;
    }

    public Long getHandledByUserId() {
        return handledByUserId;
    }

    public Instant getHandledAt() {
        return handledAt;
    }

    public void setStatus(InquiryStatus status, Long actorUserId) {
        this.status = status;
        this.handledByUserId = status == InquiryStatus.HANDLED ? actorUserId : null;
        this.handledAt = status == InquiryStatus.HANDLED ? Instant.now() : null;
    }
}
