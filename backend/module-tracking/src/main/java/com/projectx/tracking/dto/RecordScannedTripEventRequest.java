package com.projectx.tracking.dto;

import com.projectx.common.events.ChildTripEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecordScannedTripEventRequest(@NotBlank String qrToken, @NotNull ChildTripEventType eventType) {
}
