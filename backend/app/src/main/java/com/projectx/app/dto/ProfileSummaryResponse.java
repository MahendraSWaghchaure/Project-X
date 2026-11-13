package com.projectx.app.dto;

import com.projectx.common.security.Role;
import com.projectx.people.domain.DriverEmploymentStatus;
import java.util.List;

/**
 * "Who is this complainant" card: base identity plus, for a parent, their children, and for a driver, the driver profile.
 * {@code children} and {@code driver} are null when they do not apply.
 */
public record ProfileSummaryResponse(Long userId, Role role, String name, String phone, String email, Long schoolId,
                                     String schoolName, List<ChildSummary> children, DriverSummary driver) {

    public record ChildSummary(Long id, String name, String schoolName, String className) {
    }

    public record DriverSummary(Long driverId, String licenseNumber, String vehicleNo, DriverEmploymentStatus employmentStatus,
                                String address) {
    }
}
