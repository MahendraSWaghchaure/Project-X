package com.projectx.people.dto;

import com.projectx.common.geo.GeoPoint;
import com.projectx.common.geo.GeoPoints;
import com.projectx.people.domain.ParentProfile;

public record ParentResponse(
        Long id, Long invitedBySchoolId, String firstName, String lastName, String phone, String address,
        GeoPoint homeLocation) {

    public static ParentResponse from(ParentProfile parent) {
        GeoPoint home = parent.getHomeLocation() != null ? GeoPoints.toGeoPoint(parent.getHomeLocation()) : null;
        return new ParentResponse(parent.getId(), parent.getInvitedBySchoolId(), parent.getFirstName(),
                parent.getLastName(), parent.getPhone(), parent.getAddress(), home);
    }
}
