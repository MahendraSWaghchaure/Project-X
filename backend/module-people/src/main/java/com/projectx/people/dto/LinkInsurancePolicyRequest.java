package com.projectx.people.dto;

import jakarta.validation.constraints.NotNull;

public record LinkInsurancePolicyRequest(@NotNull Long insurancePolicyId) {
}
