package com.projectx.people.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateDriverRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        @NotBlank String licenseNumber,
        LocalDate licenseExpiry,
        @DecimalMin("0.0") BigDecimal baseSalaryMonthly) {
}
