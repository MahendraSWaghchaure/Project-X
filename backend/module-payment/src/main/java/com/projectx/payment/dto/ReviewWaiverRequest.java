package com.projectx.payment.dto;

import com.projectx.payment.domain.WaiverStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewWaiverRequest(@NotNull WaiverStatus status, @Size(max = 1000) String reviewNote) {
}
