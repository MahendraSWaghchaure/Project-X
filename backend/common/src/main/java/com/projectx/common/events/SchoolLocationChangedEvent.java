package com.projectx.common.events;

/**
 * Published by module-school whenever a school's location or geofence radius changes, consumed by
 * module-tracking to evict its cached GeoLookupCacheService entry. module-school can't depend on
 * module-tracking directly (wrong direction - tracking depends on school, not the reverse), so
 * this event is the seam, same convention as every other cross-module event in this codebase.
 */
public record SchoolLocationChangedEvent(Long schoolId) {
}
