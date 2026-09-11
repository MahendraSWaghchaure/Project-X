package com.projectx.app.dto;

import java.math.BigDecimal;

public record SchoolDashboardStatsResponse(
        long vehiclesTotal,
        long vehiclesActive,
        long driversTotal,
        long driversVerified,
        long independentDriversAvailable,
        long routesTotal,
        long studentsTotal,
        long tripsToday,
        long tripsInProgress,
        long tripsDelayed,
        long openGrievances,
        BigDecimal revenueThisMonth,
        long payrollPending,
        long payrollFailed) {
}
