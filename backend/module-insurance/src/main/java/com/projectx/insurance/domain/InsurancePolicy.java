package com.projectx.insurance.domain;

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

/**
 * schoolId is denormalized here (rather than resolved by joining through the subject's own
 * module) so module-insurance stays a leaf module with no compile dependency on module-school/
 * module-people - it only ever deals in ids, same discipline as Vehicle.insurancePolicyId etc.
 */
@Entity
@Table(name = "insurance_policies")
public class InsurancePolicy extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 16)
    private InsuredSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    @Column(name = "coverage_amount", precision = 14, scale = 2)
    private BigDecimal coverageAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InsurancePolicyStatus status = InsurancePolicyStatus.ACTIVE;

    protected InsurancePolicy() {
    }

    public InsurancePolicy(Long schoolId, InsuredSubjectType subjectType, Long subjectId, String provider,
                            String policyNumber, BigDecimal coverageAmount, LocalDate startDate, LocalDate endDate) {
        this.schoolId = schoolId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.provider = provider;
        this.policyNumber = policyNumber;
        this.coverageAmount = coverageAmount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public InsuredSubjectType getSubjectType() {
        return subjectType;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public String getProvider() {
        return provider;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public InsurancePolicyStatus getStatus() {
        return status;
    }

    public void setStatus(InsurancePolicyStatus status) {
        this.status = status;
    }

    public boolean isExpired(LocalDate asOf) {
        return endDate.isBefore(asOf);
    }
}
