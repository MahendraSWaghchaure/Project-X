package com.projectx.school.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalTime;

public record UpdateSchoolRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String city,
        String state,
        String pincode,
        @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String contactPhone,
        @Email String contactEmail,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Min(50) Integer geofenceRadiusMeters,
        LocalTime dismissalTime) {
}
