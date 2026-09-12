package com.projectx.school.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateRouteRequest(
        @NotBlank String name,
        @NotEmpty @Valid List<RouteStopRequest> stops,
        @Min(0) Integer zoneMinRadiusMeters,
        @Min(0) Integer zoneMaxRadiusMeters) {
}
