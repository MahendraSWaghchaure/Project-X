package com.projectx.people.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateChildRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        LocalDate dateOfBirth,
        String classSection,
        @Size(max = 8) String bloodType,
        String emergencyContactName,
        @Size(max = 32) String emergencyContactPhone,
        @Size(max = 500) String allergies) {
}
