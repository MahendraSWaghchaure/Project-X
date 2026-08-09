package com.projectx.people.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateDriverRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        @NotBlank String licenseNumber,
        LocalDate licenseExpiry,
        BigDecimal baseSalaryMonthly) {
}
