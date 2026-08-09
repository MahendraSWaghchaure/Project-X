package com.projectx.people.dto;

import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.domain.DriverProfile;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DriverResponse(
        Long id, Long schoolId, String firstName, String lastName, String phone, String licenseNumber,
        LocalDate licenseExpiry, Long insurancePolicyId, BigDecimal baseSalaryMonthly,
        DriverEmploymentStatus employmentStatus) {

    public static DriverResponse from(DriverProfile driver) {
        return new DriverResponse(driver.getId(), driver.getSchoolId(), driver.getFirstName(), driver.getLastName(),
                driver.getPhone(), driver.getLicenseNumber(), driver.getLicenseExpiry(), driver.getInsurancePolicyId(),
                driver.getBaseSalaryMonthly(), driver.getEmploymentStatus());
    }
}
