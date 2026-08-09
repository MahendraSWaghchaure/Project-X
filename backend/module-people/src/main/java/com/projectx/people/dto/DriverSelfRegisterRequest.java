package com.projectx.people.dto;

import com.projectx.school.domain.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** A privately-owned-vehicle driver signing up on their own, before any school has engaged them - see DriverService.selfRegister. */
public record DriverSelfRegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String phone,
        @NotBlank String licenseNumber,
        LocalDate licenseExpiry,

        @NotBlank String vehicleRegistrationNumber,
        @NotNull VehicleType vehicleType,
        @Min(1) int vehicleSeatCapacity) {
}
