package com.projectx.tracking.dto;

import com.projectx.tracking.domain.SafetyEventType;
import com.projectx.tracking.domain.VehicleSafetyEvent;
import java.time.Instant;

public record SafetyEventResponse(Long id, Long tripId, Long vehicleId, SafetyEventType type, Double speedKmh, Instant occurredAt) {

    public static SafetyEventResponse from(VehicleSafetyEvent event) {
        return new SafetyEventResponse(event.getId(), event.getTripId(), event.getVehicleId(), event.getType(),
                event.getSpeedKmh(), event.getOccurredAt());
    }
}
