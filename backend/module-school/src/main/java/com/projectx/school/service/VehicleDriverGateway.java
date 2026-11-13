package com.projectx.school.service;

import java.util.Collection;
import java.util.Map;

/**
 * Seam to module-people, which owns drivers and children but depends on this module (so this module cannot call it
 * directly). module-people provides the implementation; without it vehicles simply carry no driver/student data.
 */
public interface VehicleDriverGateway {

    /** driverId/driverName null when nobody is allocated; studentCount = active children riding a route assigned to this vehicle. */
    record VehicleExtras(Long driverId, String driverName, long studentCount) {
        public static final VehicleExtras NONE = new VehicleExtras(null, null, 0);
    }

    /** One batched lookup for every vehicle of a school - never per-vehicle queries. */
    Map<Long, VehicleExtras> extrasFor(Long schoolId, Collection<Long> vehicleIds);

    /**
     * Allocates {@code driverId} (a driver of {@code schoolId}) to the vehicle, releasing whichever driver held it and
     * the vehicle the driver held before. {@code driverId} null unassigns the vehicle's current driver.
     */
    void assignDriver(Long schoolId, Long vehicleId, Long driverId);
}
