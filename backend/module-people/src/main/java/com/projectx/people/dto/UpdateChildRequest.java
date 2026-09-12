package com.projectx.people.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateChildRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        LocalDate dateOfBirth,
        String classSection,
        @Size(max = 8) String bloodType,
        String emergencyContactName,
        @Size(max = 32) @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String emergencyContactPhone,
        @Size(max = 500) String allergies) {
}
