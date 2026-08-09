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
import java.time.LocalDate;

@Entity
@Table(name = "driver_profiles")
public class DriverProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "school_id", nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DriverEmploymentStatus employmentStatus = DriverEmploymentStatus.ACTIVE;

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

    public Long getInsurancePolicyId() {
        return insurancePolicyId;
    }

    public void setInsurancePolicyId(Long insurancePolicyId) {
        this.insurancePolicyId = insurancePolicyId;
    }

    public BigDecimal getBaseSalaryMonthly() {
        return baseSalaryMonthly;
    }

    public DriverEmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(DriverEmploymentStatus employmentStatus) {
        this.employmentStatus = employmentStatus;
    }
}
