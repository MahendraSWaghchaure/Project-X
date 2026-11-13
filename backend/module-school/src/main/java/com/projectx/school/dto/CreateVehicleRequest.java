package com.projectx.school.dto;

import com.projectx.common.validation.Patterns;
import com.projectx.school.domain.OwnershipType;
import com.projectx.school.domain.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Owner details are optional for a SCHOOL-owned vehicle and required (ownerName) for a PRIVATE one;
 * {@code assignedDriverId} allocates one of the school's drivers to the vehicle in the same transaction.
 */
public record CreateVehicleRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9]+$", message = "must be uppercase letters and digits only, no spaces or symbols")
        String registrationNumber,
        @NotNull VehicleType type,
        @Min(1) int seatCapacity,
        String gpsDeviceId,
        @Size(max = 255) String ownerName,
        @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String ownerPhone,
        @Size(max = 500) String ownerAddress,
        OwnershipType ownershipType,
        Long assignedDriverId) {

    /** The original four-field shape (school-owned, no driver allocated). */
    public CreateVehicleRequest(String registrationNumber, VehicleType type, int seatCapacity, String gpsDeviceId) {
        this(registrationNumber, type, seatCapacity, gpsDeviceId, null, null, null, null, null);
    }
}
