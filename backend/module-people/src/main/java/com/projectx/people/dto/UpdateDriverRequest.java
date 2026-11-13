package com.projectx.people.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code address} null keeps the stored address (the other fields are a full replace, as before). */
public record UpdateDriverRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        @NotBlank String licenseNumber,
        LocalDate licenseExpiry,
        @DecimalMin("0.0") BigDecimal baseSalaryMonthly,
        @Size(max = 500) String address) {

    public UpdateDriverRequest(String firstName, String lastName, String phone, String licenseNumber,
                               LocalDate licenseExpiry, BigDecimal baseSalaryMonthly) {
        this(firstName, lastName, phone, licenseNumber, licenseExpiry, baseSalaryMonthly, null);
    }
}
