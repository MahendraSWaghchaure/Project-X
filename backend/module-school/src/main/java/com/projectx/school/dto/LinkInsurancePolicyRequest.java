package com.projectx.school.dto;

import jakarta.validation.constraints.NotNull;

public record LinkInsurancePolicyRequest(@NotNull Long insurancePolicyId) {
}
