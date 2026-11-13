package com.projectx.app.dto;

import com.projectx.grievance.dto.GrievanceResponse;
import com.projectx.people.domain.DriverVerificationStatus;
import com.projectx.school.dto.SchoolResponse;
import java.util.List;

/**
 * B1: everything the platform admin's school-detail page needs in one call. {@code drivers} and
 * {@code recentGrievances} are capped lists (see SchoolOverviewService), not the whole table -
 * same "indexed counts, not full-list fetches" rule as DashboardStatsService.
 *
 * <p>{@code recentGrievances} (newest 5, enriched the same way the grievances table is) is an
 * ADDITIVE field beyond the plan's literal {@code {school, drivers, vehicleCount, studentCount,
 * subscribedCount, openGrievances}} shape - see PLAN-phase3's Contract deviations.
 */
public record SchoolOverviewResponse(
        SchoolResponse school, List<DriverRow> drivers, long vehicleCount, long studentCount, long subscribedCount,
        long openGrievances, List<GrievanceResponse> recentGrievances) {

    public record DriverRow(Long driverId, String name, String phone, DriverVerificationStatus verificationStatus,
                            String vehicleRegistrationNumber) {
    }
}
