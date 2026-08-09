package com.projectx.people.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdateChildRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        LocalDate dateOfBirth,
        String classSection) {
}
