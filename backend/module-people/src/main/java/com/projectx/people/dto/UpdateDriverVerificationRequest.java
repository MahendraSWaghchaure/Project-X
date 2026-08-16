package com.projectx.people.dto;

import com.projectx.people.domain.DriverVerificationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverVerificationRequest(@NotNull DriverVerificationStatus status) {
}
