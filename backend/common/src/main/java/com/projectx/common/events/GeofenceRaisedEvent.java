package com.projectx.common.events;

import java.time.Instant;

/**
 * Published by module-tracking, consumed by module-notification. Deliberately carries plain ids
 * (not entities) so notification never needs a compile dependency on tracking's internal model.
 *
 * @param childId set only for per-stop events (APPROACHING_STOP/ARRIVED_STOP); null for
 *                school-wide and delay events, which fan out to every child on the trip.
 */
public record GeofenceRaisedEvent(
        Long tripId, Long vehicleId, Long schoolId, Long routeId, Long routeStopId, Long childId,
        GeofenceEventType type, Instant occurredAt) {
}
