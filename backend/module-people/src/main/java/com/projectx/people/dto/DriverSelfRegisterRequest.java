package com.projectx.people.dto;

import com.projectx.common.validation.Patterns;
import com.projectx.school.domain.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** A privately-owned-vehicle driver signing up on their own, before any school has engaged them - see DriverService.selfRegister. */
public record DriverSelfRegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 255) String password,
        @NotBlank @Size(max = 32) @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        @NotBlank @Size(max = 64) String licenseNumber,
        LocalDate licenseExpiry,

        @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Z0-9]+$", message = "must be uppercase letters and digits only, no spaces or symbols")
        String vehicleRegistrationNumber,
        @NotNull VehicleType vehicleType,
        @Min(1) int vehicleSeatCapacity) {
}
