package com.projectx.school.dto;

import com.projectx.school.domain.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateVehicleRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9]+$", message = "must be uppercase letters and digits only, no spaces or symbols")
        String registrationNumber,
        @NotNull VehicleType type,
        @Min(1) int seatCapacity,
        String gpsDeviceId) {
}
