package com.projectx.people.dto;

import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.DriverVerificationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** {@code vehicleRegistrationNumber} (the allocated vehicle's number) is filled by the list/detail endpoints. */
public record DriverResponse(
        Long id, Long schoolId, String firstName, String lastName, String phone, String licenseNumber,
        LocalDate licenseExpiry, Long insurancePolicyId, BigDecimal baseSalaryMonthly, Long vehicleId,
        DriverEmploymentStatus employmentStatus, DriverVerificationStatus verificationStatus, Instant verifiedAt,
        String address, String statusReason, Instant statusChangedAt, String vehicleRegistrationNumber) {

    public static DriverResponse from(DriverProfile driver) {
        return from(driver, null);
    }

    public static DriverResponse from(DriverProfile driver, String vehicleRegistrationNumber) {
        return new DriverResponse(driver.getId(), driver.getSchoolId(), driver.getFirstName(), driver.getLastName(),
                driver.getPhone(), driver.getLicenseNumber(), driver.getLicenseExpiry(), driver.getInsurancePolicyId(),
                driver.getBaseSalaryMonthly(), driver.getVehicleId(), driver.getEmploymentStatus(),
                driver.getVerificationStatus(), driver.getVerifiedAt(), driver.getAddress(), driver.getStatusReason(),
                driver.getStatusChangedAt(), vehicleRegistrationNumber);
    }
}
