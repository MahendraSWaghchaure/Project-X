package com.projectx.insurance.dto;

import com.projectx.insurance.domain.InsuredSubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInsurancePolicyRequest(
        @NotNull InsuredSubjectType subjectType,
        @NotNull Long subjectId,
        @NotBlank String provider,
        @NotBlank String policyNumber,
        BigDecimal coverageAmount,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
