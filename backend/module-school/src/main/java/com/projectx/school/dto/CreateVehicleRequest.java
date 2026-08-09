package com.projectx.school.dto;

import com.projectx.school.domain.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVehicleRequest(
        @NotBlank String registrationNumber,
        @NotNull VehicleType type,
        @Min(1) int seatCapacity,
        String gpsDeviceId) {
}
