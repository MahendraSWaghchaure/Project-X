package com.projectx.school.dto;

import com.projectx.school.domain.Route;
import java.util.List;

public record RouteResponse(
        Long id, Long schoolId, String name, Long vehicleId, Long driverId, boolean active,
        int zoneMinRadiusMeters, Integer zoneMaxRadiusMeters, List<RouteStopResponse> stops) {

    public static RouteResponse from(Route route, List<RouteStopResponse> stops) {
        return new RouteResponse(route.getId(), route.getSchoolId(), route.getName(), route.getVehicleId(),
                route.getDriverId(), route.isActive(), route.getZoneMinRadiusMeters(), route.getZoneMaxRadiusMeters(),
                stops);
    }
}
