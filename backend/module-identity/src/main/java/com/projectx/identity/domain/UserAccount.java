package com.projectx.identity.domain;

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

/**
 * Authentication identity only - one row per human, regardless of role. Role-specific profile
 * data (license number, home address, etc.) lives in the owning domain module
 * (module-school / module-people), keyed by userId.
 *
 * <p>{@code schoolId} is intentionally denormalized here (not just on the profile table) so the
 * JWT can be issued with tenant scope without module-identity depending on module-school/people.
 * It's null for PLATFORM_ADMIN (sees every school) and PARENT (scoped by guardianship, not a
 * single school).
 */
@Entity
@Table(name = "users", indexes = {
        @jakarta.persistence.Index(name = "idx_users_school_id", columnList = "school_id")
})
public class UserAccount extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Column(name = "school_id")
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    protected UserAccount() {
    }

    public UserAccount(String email, String phone, String passwordHash, Role role, Long schoolId) {
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.schoolId = schoolId;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public Role getRole() {
        return role;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    /** Used when an independent driver is claimed by a school - see module-people's DriverService.claimDriver. */
    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
