package com.projectx.school.dto;

import com.projectx.school.domain.Vehicle;
import com.projectx.school.domain.VehicleStatus;
import com.projectx.school.domain.VehicleType;

public record VehicleResponse(
        Long id, Long schoolId, String registrationNumber, VehicleType type, int seatCapacity,
        String gpsDeviceId, Long insurancePolicyId, VehicleStatus status) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getSchoolId(), vehicle.getRegistrationNumber(),
                vehicle.getType(), vehicle.getSeatCapacity(), vehicle.getGpsDeviceId(),
                vehicle.getInsurancePolicyId(), vehicle.getStatus());
    }
}
