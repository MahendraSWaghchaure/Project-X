package com.projectx.tracking.dto;

import com.projectx.common.events.ChildTripEventType;
import com.projectx.tracking.domain.ChildTripEvent;
import java.time.Instant;

public record ChildTripEventResponse(Long id, Long tripId, Long childId, ChildTripEventType eventType,
                                      Long confirmedByUserId, Instant occurredAt) {

    public static ChildTripEventResponse from(ChildTripEvent event) {
        return new ChildTripEventResponse(event.getId(), event.getTripId(), event.getChildId(), event.getEventType(),
                event.getConfirmedByUserId(), event.getOccurredAt());
    }
}
