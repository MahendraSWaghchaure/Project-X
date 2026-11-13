package com.projectx.school.dto;

import com.projectx.common.geo.GeoPoint;
import com.projectx.common.geo.GeoPoints;
import com.projectx.school.domain.RouteStop;
import java.time.LocalTime;

public record RouteStopResponse(Long id, int sequenceNumber, String label, GeoPoint location, int radiusMeters,
                                LocalTime scheduledTime) {

    public static RouteStopResponse from(RouteStop stop) {
        return new RouteStopResponse(stop.getId(), stop.getSequenceNumber(), stop.getLabel(),
                GeoPoints.toGeoPoint(stop.getLocation()), stop.getRadiusMeters(), stop.getScheduledTime());
    }
}
