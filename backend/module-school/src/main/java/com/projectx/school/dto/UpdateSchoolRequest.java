package com.projectx.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalTime;

public record UpdateSchoolRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        String state,
        String pincode,
        String contactPhone,
        @Email String contactEmail,
        Double latitude,
        Double longitude,
        Integer geofenceRadiusMeters,
        LocalTime dismissalTime) {
}
