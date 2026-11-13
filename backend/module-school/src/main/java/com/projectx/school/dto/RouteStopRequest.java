package com.projectx.school.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * {@code id} is only meaningful on route update: a stop carrying the id of an existing stop of that route is updated
 * in place (children keep pointing at it); a stop without one is new; existing stops absent from the request are removed.
 * {@code scheduledTime} is the planned arrival time shown in the stops list.
 */
public record RouteStopRequest(
        Long id,
        @Min(0) int sequenceNumber,
        @NotBlank String label,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Min(20) Integer radiusMeters,
        LocalTime scheduledTime) {

    /** The original five-field shape (new stop, no planned time). */
    public RouteStopRequest(int sequenceNumber, String label, Double latitude, Double longitude, Integer radiusMeters) {
        this(null, sequenceNumber, label, latitude, longitude, radiusMeters, null);
    }
}
