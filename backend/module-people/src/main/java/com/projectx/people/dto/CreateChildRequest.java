package com.projectx.people.dto;

import jakarta.validation.constraints.NotBlank;
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
        @Size(max = 32) String emergencyContactPhone,
        @Size(max = 500) String allergies) {
}
