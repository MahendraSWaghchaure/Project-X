package com.projectx.people.dto;

import com.projectx.people.domain.DriverEmploymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code reason} is mandatory for SUSPENDED and TERMINATED (stored as statusReason), cleared when moving back to ACTIVE. */
public record UpdateDriverEmploymentStatusRequest(@NotNull DriverEmploymentStatus status, @Size(max = 500) String reason) {

    public UpdateDriverEmploymentStatusRequest(DriverEmploymentStatus status) {
        this(status, null);
    }
}
