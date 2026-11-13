package com.projectx.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A guardian-consent request sent by SMS. Only the SHA-256 of the token is stored (the raw token exists only in the SMS).
 * Single use: {@code usedAt}/{@code response} are set exactly once, by an atomic conditional UPDATE (see ConsentTokenRepository).
 */
@Entity
@Table(name = "consent_tokens")
public class ConsentToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column
    private Boolean response;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ConsentToken() {
    }

    public ConsentToken(Long studentId, Long schoolId, String tokenHash, Instant expiresAt, Long createdByUserId) {
        this.studentId = studentId;
        this.schoolId = schoolId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdByUserId = createdByUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Boolean getResponse() {
        return response;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
