package com.projectx.grievance.service;

import com.projectx.common.security.Role;
import java.util.Collection;
import java.util.Map;

/**
 * Seam to the modules that own people, vehicles, children and schools. This module is a leaf (no compile dependency on
 * them); the composition root ({@code app}) provides the implementation. Without one, grievances simply carry no display
 * names and links are not verified.
 */
public interface GrievanceDirectory {

    /** Display name for each user id (parent / driver / school-admin name, falling back to the login email). */
    Map<Long, String> userNames(Collection<Long> userIds);

    /** Driver display names keyed by DriverProfile id. */
    Map<Long, String> driverNames(Collection<Long> driverProfileIds);

    /** Registration numbers keyed by vehicle id. */
    Map<Long, String> vehicleNumbers(Collection<Long> vehicleIds);

    /** Child names keyed by child id. */
    Map<Long, String> childNames(Collection<Long> childIds);

    /** School names keyed by school id. */
    Map<Long, String> schoolNames(Collection<Long> schoolIds);

    /**
     * Verifies that the optional links make sense for this submitter, throwing IllegalArgumentException (400) or
     * ForbiddenOperationException (403) otherwise: a child must be the submitter's own (parent: guardian of the child),
     * or belong to their school (driver / school admin); a vehicle, driver or trip must belong to {@code schoolId}.
     * Foreign ids are reported exactly like missing ones.
     */
    void validateLinks(Role role, Long userId, Long schoolId, Long vehicleId, Long driverId, Long childId, Long tripId);
}
