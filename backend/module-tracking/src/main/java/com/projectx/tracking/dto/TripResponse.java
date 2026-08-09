package com.projectx.tracking.dto;

import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.domain.TripDirection;
import com.projectx.tracking.domain.TripStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TripResponse(
        Long id, Long schoolId, Long routeId, Long vehicleId, Long driverId, TripDirection direction,
        LocalDate tripDate, TripStatus status, Instant startedAt, Instant endedAt) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(trip.getId(), trip.getSchoolId(), trip.getRouteId(), trip.getVehicleId(),
                trip.getDriverId(), trip.getDirection(), trip.getTripDate(), trip.getStatus(),
                trip.getStartedAt(), trip.getEndedAt());
    }
}
