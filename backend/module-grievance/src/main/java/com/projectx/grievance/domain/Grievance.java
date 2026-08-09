package com.projectx.grievance.domain;

import com.projectx.common.domain.Auditable;
import com.projectx.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Raised by a parent, driver, or school admin about anything trip/vehicle/conduct/payment related;
 * resolved by the school admin of the school it concerns, or by the platform admin for any school.
 * raisedByEmail is denormalized (copied from the JWT at creation time) rather than joined from
 * module-identity, keeping this a leaf module with no compile dependency on identity/people/school -
 * same cross-module-reference-by-id-only discipline used everywhere else in this codebase.
 */
@Entity
@Table(name = "grievances")
public class Grievance extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "raised_by_user_id", nullable = false)
    private Long raisedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "raised_by_role", nullable = false, length = 16)
    private Role raisedByRole;

    @Column(name = "raised_by_email", nullable = false)
    private String raisedByEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GrievanceCategory category;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GrievanceStatus status = GrievanceStatus.OPEN;

    @Column(name = "resolution_note", length = 2000)
    private String resolutionNote;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Grievance() {
    }

    public Grievance(Long schoolId, Long raisedByUserId, Role raisedByRole, String raisedByEmail,
                      GrievanceCategory category, String subject, String description) {
        this.schoolId = schoolId;
        this.raisedByUserId = raisedByUserId;
        this.raisedByRole = raisedByRole;
        this.raisedByEmail = raisedByEmail;
        this.category = category;
        this.subject = subject;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Long getRaisedByUserId() {
        return raisedByUserId;
    }

    public Role getRaisedByRole() {
        return raisedByRole;
    }

    public String getRaisedByEmail() {
        return raisedByEmail;
    }

    public GrievanceCategory getCategory() {
        return category;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public GrievanceStatus getStatus() {
        return status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Long getResolvedByUserId() {
        return resolvedByUserId;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void resolve(GrievanceStatus newStatus, String resolutionNote, Long resolvedByUserId) {
        this.status = newStatus;
        this.resolutionNote = resolutionNote;
        this.resolvedByUserId = resolvedByUserId;
        this.resolvedAt = Instant.now();
    }
}
