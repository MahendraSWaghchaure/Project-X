package com.projectx.school.dto;

import com.projectx.school.domain.OwnershipType;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.domain.VehicleStatus;
import com.projectx.school.domain.VehicleType;

/**
 * {@code assignedDriverId}/{@code driverName}/{@code studentCount} are filled only by the list/detail/create/update
 * endpoints (module-people owns drivers and children - see VehicleDriverGateway); {@code routeName} joins the names of
 * the routes this vehicle is assigned to. Everywhere else they are null / 0.
 */
public record VehicleResponse(
        Long id, Long schoolId, String registrationNumber, VehicleType type, int seatCapacity,
        String gpsDeviceId, Long insurancePolicyId, VehicleStatus status,
        String ownerName, String ownerPhone, String ownerAddress, OwnershipType ownershipType,
        Long assignedDriverId, String driverName, String routeName, long studentCount) {

    public static VehicleResponse from(Vehicle vehicle) {
        return from(vehicle, null, null, null, 0);
    }

    public static VehicleResponse from(Vehicle vehicle, Long assignedDriverId, String driverName, String routeName,
                                       long studentCount) {
        return new VehicleResponse(vehicle.getId(), vehicle.getSchoolId(), vehicle.getRegistrationNumber(),
                vehicle.getType(), vehicle.getSeatCapacity(), vehicle.getGpsDeviceId(),
                vehicle.getInsurancePolicyId(), vehicle.getStatus(), vehicle.getOwnerName(), vehicle.getOwnerPhone(),
                vehicle.getOwnerAddress(), vehicle.getOwnershipType(), assignedDriverId, driverName, routeName,
                studentCount);
    }
}
