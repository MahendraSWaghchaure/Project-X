package com.projectx.tracking.dto;

import com.projectx.tracking.domain.SosEvent;
import java.time.Instant;

public record SosEventResponse(
        Long id, Long tripId, Long vehicleId, Long schoolId, Long routeId, Long triggeredByUserId,
        String note, Instant triggeredAt, Instant resolvedAt, boolean resolved) {

    public static SosEventResponse from(SosEvent event) {
        return new SosEventResponse(event.getId(), event.getTripId(), event.getVehicleId(), event.getSchoolId(),
                event.getRouteId(), event.getTriggeredByUserId(), event.getNote(), event.getTriggeredAt(),
                event.getResolvedAt(), event.isResolved());
    }
}
