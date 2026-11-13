package com.projectx.people.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateDriverRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        @NotBlank String licenseNumber,
        LocalDate licenseExpiry,
        @DecimalMin("0.0") BigDecimal baseSalaryMonthly,
        @Size(max = 500) String address) {

    /** The original eight-field shape (no address). */
    public CreateDriverRequest(String firstName, String lastName, String email, String password, String phone,
                               String licenseNumber, LocalDate licenseExpiry, BigDecimal baseSalaryMonthly) {
        this(firstName, lastName, email, password, phone, licenseNumber, licenseExpiry, baseSalaryMonthly, null);
    }
}
