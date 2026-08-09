package com.projectx.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertBankDetailsRequest(
        @NotBlank @Size(max = 255) String accountHolderName,
        @NotBlank @Pattern(regexp = "\\d{6,20}", message = "must be 6-20 digits") String accountNumber,
        @NotBlank @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}", message = "must be a valid IFSC code, e.g. HDFC0001234") String ifscCode) {
}
