package com.projectx.school.dto;

/** {@code driverId} null = unassign whichever driver currently holds the vehicle. */
public record AssignVehicleDriverRequest(Long driverId) {
}
