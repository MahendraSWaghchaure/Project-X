package com.projectx.tracking.event;

import java.time.Instant;

/**
 * Published the moment a raw ping is persisted, consumed asynchronously to do the actual work
 * (geofence checks, safety-flag checks, cache update, live broadcast). Carries plain values, not
 * entities, same convention as GeofenceRaisedEvent - safe to hand across the async boundary with
 * no risk of touching a detached/lazy entity from a different thread.
 */
public record LocationIngestedEvent(
        Long vehicleId, Long tripId, double latitude, double longitude, Double speedKmh, Double headingDegrees,
        Instant recordedAt, Double previousSpeedKmh, Instant previousRecordedAt) {
}
