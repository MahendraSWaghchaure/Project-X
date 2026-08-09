package com.projectx.people.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** primaryParentId is required on the school-admin enrollment endpoint and ignored (self-derived) on the parent self-service endpoint. */
public record CreateChildRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        LocalDate dateOfBirth,
        String classSection,
        Long primaryParentId) {
}
