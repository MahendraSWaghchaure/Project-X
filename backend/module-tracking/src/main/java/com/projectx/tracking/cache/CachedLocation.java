package com.projectx.tracking.cache;

import java.time.Instant;

/** What the Redis "current position" cache stores - read paths (map views, dashboards) hit this, never the raw ping table. */
public record CachedLocation(
        Long vehicleId, Long tripId, double latitude, double longitude, Double speedKmh, Double headingDegrees,
        Instant recordedAt) {
}
