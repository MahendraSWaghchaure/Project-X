package com.projectx.people.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * schoolId is nullable: an independently-registered (privately-owned-vehicle) driver has no
 * school until one claims them - see DriverService.selfRegister/claimDriver. vehicleId is the
 * vehicle they registered for themselves in that case (cross-module id reference, module-school).
 */
@Entity
@Table(name = "driver_profiles")
public class DriverProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "insurance_policy_id")
    private Long insurancePolicyId;

    @Column(name = "base_salary_monthly", precision = 12, scale = 2)
    private BigDecimal baseSalaryMonthly;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DriverEmploymentStatus employmentStatus = DriverEmploymentStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 16)
    private DriverVerificationStatus verificationStatus = DriverVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by_user_id")
    private Long verifiedByUserId;

    protected DriverProfile() {
    }

    public DriverProfile(Long userId, Long schoolId, String firstName, String lastName, String phone,
                          String licenseNumber, LocalDate licenseExpiry, BigDecimal baseSalaryMonthly) {
        this.userId = userId;
        this.schoolId = schoolId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.licenseExpiry = licenseExpiry;
        this.baseSalaryMonthly = baseSalaryMonthly;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public void updateDetails(String firstName, String lastName, String phone, String licenseNumber,
                               LocalDate licenseExpiry, BigDecimal baseSalaryMonthly) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.licenseExpiry = licenseExpiry;
        this.baseSalaryMonthly = baseSalaryMonthly;
    }

    public Long getInsurancePolicyId() {
        return insurancePolicyId;
    }

    public void setInsurancePolicyId(Long insurancePolicyId) {
        this.insurancePolicyId = insurancePolicyId;
    }

    public BigDecimal getBaseSalaryMonthly() {
        return baseSalaryMonthly;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public DriverEmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(DriverEmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public boolean isIndependent() {
        return schoolId == null;
    }

    public DriverVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Long getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void updateVerification(DriverVerificationStatus status, Long verifiedByUserId) {
        this.verificationStatus = status;
        this.verifiedAt = Instant.now();
        this.verifiedByUserId = verifiedByUserId;
    }
}
