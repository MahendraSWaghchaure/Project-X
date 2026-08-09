package com.projectx.school.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Configures which band of distance-from-school this route auto-allocates children from - see Route.coversDistance. */
public record UpdateRouteZoneRequest(@NotNull @Min(0) Integer zoneMinRadiusMeters, Integer zoneMaxRadiusMeters) {
}
