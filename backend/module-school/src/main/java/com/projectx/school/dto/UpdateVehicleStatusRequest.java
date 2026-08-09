package com.projectx.school.dto;

import com.projectx.school.domain.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVehicleStatusRequest(@NotNull VehicleStatus status) {
}
