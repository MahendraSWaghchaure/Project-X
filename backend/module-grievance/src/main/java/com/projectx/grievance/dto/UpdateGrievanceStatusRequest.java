package com.projectx.grievance.dto;

import com.projectx.grievance.domain.GrievanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGrievanceStatusRequest(@NotNull GrievanceStatus status, @Size(max = 2000) String resolutionNote) {
}
