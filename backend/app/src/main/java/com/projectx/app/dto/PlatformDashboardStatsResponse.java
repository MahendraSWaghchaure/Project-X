package com.projectx.app.dto;

public record PlatformDashboardStatsResponse(
        long schoolsTotal,
        long schoolsActive,
        long schoolsSuspended,
        long independentDriversUnclaimed,
        long openGrievances,
        long pendingWaivers) {
}
