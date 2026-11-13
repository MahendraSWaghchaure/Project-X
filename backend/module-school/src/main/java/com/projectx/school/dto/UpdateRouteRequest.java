package com.projectx.school.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Full replace of a route's editable parts: name, active flag, zone and the ordered stop list (see RouteStopRequest.id for
 * how existing stops are matched). {@code active} null keeps the stored value.
 */
public record UpdateRouteRequest(
        @NotBlank String name,
        @NotEmpty @Valid List<RouteStopRequest> stops,
        @Min(0) Integer zoneMinRadiusMeters,
        @Min(0) Integer zoneMaxRadiusMeters,
        Boolean active) {
}
