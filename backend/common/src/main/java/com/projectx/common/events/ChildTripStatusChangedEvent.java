package com.projectx.common.events;

import java.time.Instant;

/** Published by module-tracking whenever a driver or parent marks a child's boarding/drop status. */
public record ChildTripStatusChangedEvent(
        Long tripId, Long childId, Long schoolId, ChildTripEventType type, Long confirmedByUserId, Instant occurredAt) {
}
