package com.projectx.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWaiverRequest(@NotNull Long feeInvoiceId, @NotBlank @Size(max = 2000) String reason) {
}
