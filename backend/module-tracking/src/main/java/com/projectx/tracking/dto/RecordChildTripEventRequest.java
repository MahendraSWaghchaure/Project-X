package com.projectx.tracking.dto;

import com.projectx.common.events.ChildTripEventType;
import jakarta.validation.constraints.NotNull;

public record RecordChildTripEventRequest(@NotNull Long childId, @NotNull ChildTripEventType eventType) {
}
