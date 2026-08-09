package com.projectx.people.dto;

import jakarta.validation.constraints.NotNull;

public record AssignChildRouteStopRequest(@NotNull Long routeId, @NotNull Long routeStopId) {
}
