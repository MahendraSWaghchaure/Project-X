package com.projectx.people.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** primaryParentId is required on the school-admin enrollment endpoint and ignored (self-derived) on the parent self-service endpoint. */
public record CreateChildRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        LocalDate dateOfBirth,
        String classSection,
        Long primaryParentId,
        @Size(max = 8) String bloodType,
        String emergencyContactName,
        @Size(max = 32) @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String emergencyContactPhone,
        @Size(max = 500) String allergies) {
}
