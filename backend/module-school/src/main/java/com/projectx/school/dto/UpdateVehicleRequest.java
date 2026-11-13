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
 * Full replace of the editable fields. Owner contact fields are replaced as sent (null clears); {@code ownershipType}
 * null keeps the stored value. {@code assignedDriverId} null = leave the allocation unchanged (use
 * {@code PUT .../vehicles/{id}/driver} to unassign explicitly).
 */
public record UpdateVehicleRequest(
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

    public UpdateVehicleRequest(String registrationNumber, VehicleType type, int seatCapacity, String gpsDeviceId) {
        this(registrationNumber, type, seatCapacity, gpsDeviceId, null, null, null, null, null);
    }
}
