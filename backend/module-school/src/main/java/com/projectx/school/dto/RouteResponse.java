package com.projectx.school.dto;

import com.projectx.school.domain.Route;
import java.util.List;

/**
 * {@code stopCount} is the number of stops (== stops.size()); {@code status} is ACTIVE or INACTIVE (from {@code active});
 * {@code vehicleRegistrationNumber} is filled by the list/detail endpoints.
 */
public record RouteResponse(
        Long id, Long schoolId, String name, Long vehicleId, Long driverId, boolean active,
        int zoneMinRadiusMeters, Integer zoneMaxRadiusMeters, List<RouteStopResponse> stops,
        String vehicleRegistrationNumber, int stopCount, String status) {

    public static RouteResponse from(Route route, List<RouteStopResponse> stops) {
        return from(route, stops, null);
    }

    public static RouteResponse from(Route route, List<RouteStopResponse> stops, String vehicleRegistrationNumber) {
        return new RouteResponse(route.getId(), route.getSchoolId(), route.getName(), route.getVehicleId(),
                route.getDriverId(), route.isActive(), route.getZoneMinRadiusMeters(), route.getZoneMaxRadiusMeters(),
                stops, vehicleRegistrationNumber, stops.size(), route.isActive() ? "ACTIVE" : "INACTIVE");
    }
}
