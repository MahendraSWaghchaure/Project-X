package com.projectx.tracking.dto;

import com.projectx.tracking.domain.TripDirection;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateTripRequest(
        @NotNull Long routeId,
        @NotNull TripDirection direction,
        @NotNull @FutureOrPresent LocalDate tripDate) {
}
