package com.projectx.people.dto;

import com.projectx.people.domain.DriverEmploymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverEmploymentStatusRequest(@NotNull DriverEmploymentStatus status) {
}
