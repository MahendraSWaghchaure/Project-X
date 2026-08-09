package com.projectx.payment.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PayInvoicesRequest(@NotEmpty List<Long> feeInvoiceIds) {
}
