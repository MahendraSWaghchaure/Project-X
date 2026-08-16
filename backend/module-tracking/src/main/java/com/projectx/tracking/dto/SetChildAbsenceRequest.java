package com.projectx.tracking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SetChildAbsenceRequest(@NotNull @FutureOrPresent LocalDate date) {
}
