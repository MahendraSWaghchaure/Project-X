package com.projectx.hardware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record VehicleTelemetrySampleRequest(
        @NotBlank String deviceId,
        Double speedKmh,
        boolean harshBraking,
        boolean doorOpen,
        @NotNull Instant timestamp) {
}
