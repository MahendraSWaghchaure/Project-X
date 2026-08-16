package com.projectx.common.events;

import java.time.Instant;

/**
 * Published by module-tracking when a driver triggers the panic button, consumed by
 * module-notification. Carries plain ids, not entities - same reasoning as GeofenceRaisedEvent.
 */
public record SosRaisedEvent(
        Long sosEventId, Long tripId, Long vehicleId, Long schoolId, Long routeId, Long triggeredByUserId,
        String note, Instant occurredAt) {
}
