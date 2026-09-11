package com.projectx.school.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRouteRequest(@NotNull Long vehicleId, @NotNull Long driverId) {
}
