package com.projectx.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateSchoolRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        String state,
        String pincode,
        String contactPhone,
        @Email String contactEmail,
        Double latitude,
        Double longitude,
        @Min(50) Integer geofenceRadiusMeters,
        LocalTime dismissalTime,

        @NotBlank String adminFirstName,
        @NotBlank String adminLastName,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 8) String adminPassword) {
}
