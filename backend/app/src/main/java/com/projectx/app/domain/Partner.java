package com.projectx.app.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * F1: a lightweight placeholder for a prospective insurance company / legal advisor / RTO agent
 * the platform is "in talks" with - lets platform admin record a lead exists, nothing more.
 * {@code status} is always {@link PartnerStatus#COMING_SOON}; no onboarding, policy issuance,
 * payment or driver-facing legal-request flow is built on top of this yet.
 */
@Entity
@Table(name = "partners")
public class Partner extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PartnerType type;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PartnerStatus status = PartnerStatus.COMING_SOON;

    protected Partner() {
    }

    public Partner(PartnerType type, String name, String contactEmail, String contactPhone, String notes) {
        this.type = type;
        this.name = name;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.notes = notes;
    }

    public void updateDetails(String name, String contactEmail, String contactPhone, String notes) {
        this.name = name;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public PartnerType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getNotes() {
        return notes;
    }

    public PartnerStatus getStatus() {
        return status;
    }
}
