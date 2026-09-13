package com.projectx.school.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateSchoolRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        String state,
        String pincode,
        Long districtId,
        @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String contactPhone,
        @Email String contactEmail,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Min(50) Integer geofenceRadiusMeters,
        LocalTime dismissalTime,

        @NotBlank String adminFirstName,
        @NotBlank String adminLastName,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 8) String adminPassword) {
}
