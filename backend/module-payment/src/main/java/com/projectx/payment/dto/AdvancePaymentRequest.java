package com.projectx.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Pays for the next N not-yet-billed months of a child's fee upfront, at a discount - see AdvancePaymentDiscount. */
public record AdvancePaymentRequest(@NotNull Long childId, @Min(1) @Max(12) int months) {
}
