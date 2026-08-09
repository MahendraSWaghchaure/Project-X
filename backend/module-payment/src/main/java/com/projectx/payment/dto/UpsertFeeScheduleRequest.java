package com.projectx.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpsertFeeScheduleRequest(@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal monthlyAmount) {
}
