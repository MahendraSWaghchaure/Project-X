package com.projectx.app.web;

import com.projectx.app.dto.PlatformDashboardStatsResponse;
import com.projectx.app.dto.SchoolDashboardStatsResponse;
import com.projectx.app.service.DashboardStatsService;
import com.projectx.common.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    public DashboardStatsController(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = dashboardStatsService;
    }

    @GetMapping("/platform-admin/dashboard-stats")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PlatformDashboardStatsResponse> platformStats() {
        return ApiResponse.ok(dashboardStatsService.platformStats());
    }

    @GetMapping("/schools/{schoolId}/dashboard-stats")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<SchoolDashboardStatsResponse> schoolStats(@PathVariable Long schoolId) {
        return ApiResponse.ok(dashboardStatsService.schoolStats(schoolId));
    }
}
