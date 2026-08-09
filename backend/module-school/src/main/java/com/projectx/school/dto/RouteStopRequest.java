package com.projectx.school.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RouteStopRequest(
        @Min(0) int sequenceNumber,
        @NotBlank String label,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @Min(20) Integer radiusMeters) {
}
