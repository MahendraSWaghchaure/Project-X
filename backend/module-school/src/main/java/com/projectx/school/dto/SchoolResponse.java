package com.projectx.school.dto;

import com.projectx.common.geo.GeoPoint;
import com.projectx.school.domain.School;
import com.projectx.school.domain.SchoolStatus;
import java.time.Instant;
import java.time.LocalTime;

public record SchoolResponse(
        Long id, String name, String address, String city, String state, String pincode, Long districtId,
        String contactPhone, String contactEmail, GeoPoint location, int geofenceRadiusMeters,
        LocalTime dismissalTime, SchoolStatus status, String statusReason, Instant statusChangedAt) {

    public static SchoolResponse from(School school) {
        GeoPoint location = school.getLocation() != null
                ? com.projectx.common.geo.GeoPoints.toGeoPoint(school.getLocation())
                : null;
        return new SchoolResponse(school.getId(), school.getName(), school.getAddress(), school.getCity(),
                school.getState(), school.getPincode(), school.getDistrictId(), school.getContactPhone(),
                school.getContactEmail(), location, school.getGeofenceRadiusMeters(), school.getDismissalTime(),
                school.getStatus(), school.getStatusReason(), school.getStatusChangedAt());
    }
}
