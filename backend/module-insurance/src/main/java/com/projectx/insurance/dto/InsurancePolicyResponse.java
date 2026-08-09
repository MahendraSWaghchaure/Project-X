package com.projectx.insurance.dto;

import com.projectx.insurance.domain.InsurancePolicy;
import com.projectx.insurance.domain.InsurancePolicyStatus;
import com.projectx.insurance.domain.InsuredSubjectType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InsurancePolicyResponse(
        Long id, Long schoolId, InsuredSubjectType subjectType, Long subjectId, String provider, String policyNumber,
        BigDecimal coverageAmount, LocalDate startDate, LocalDate endDate, InsurancePolicyStatus status) {

    public static InsurancePolicyResponse from(InsurancePolicy policy) {
        return new InsurancePolicyResponse(policy.getId(), policy.getSchoolId(), policy.getSubjectType(),
                policy.getSubjectId(), policy.getProvider(), policy.getPolicyNumber(), policy.getCoverageAmount(),
                policy.getStartDate(), policy.getEndDate(), policy.getStatus());
    }
}
