package com.projectx.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SetIncentiveRequest(@NotNull @DecimalMin(value = "0.0") BigDecimal incentiveAmount) {
}
