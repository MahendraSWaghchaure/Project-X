package com.projectx.payment.domain;

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

/** A parent's request to have an invoice's late fee forgiven, with the emergency reason attached - reviewed by the platform admin. */
@Entity
@Table(name = "late_fee_waiver_requests")
public class LateFeeWaiverRequest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_invoice_id", nullable = false)
    private Long feeInvoiceId;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WaiverStatus status = WaiverStatus.PENDING;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    protected LateFeeWaiverRequest() {
    }

    public LateFeeWaiverRequest(Long feeInvoiceId, Long parentId, Long schoolId, String reason) {
        this.feeInvoiceId = feeInvoiceId;
        this.parentId = parentId;
        this.schoolId = schoolId;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getFeeInvoiceId() {
        return feeInvoiceId;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getReason() {
        return reason;
    }

    public WaiverStatus getStatus() {
        return status;
    }

    public Long getReviewedByUserId() {
        return reviewedByUserId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void approve(Long reviewerUserId, String note) {
        this.status = WaiverStatus.APPROVED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = Instant.now();
        this.reviewNote = note;
    }

    public void reject(Long reviewerUserId, String note) {
        this.status = WaiverStatus.REJECTED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = Instant.now();
        this.reviewNote = note;
    }
}
