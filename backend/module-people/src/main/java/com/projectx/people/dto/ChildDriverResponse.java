package com.projectx.people.dto;

import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.DriverVerificationStatus;
import java.time.Instant;

/** What a parent is allowed to see about the driver assigned to their child's vehicle - trust info, not the full admin-facing profile. */
public record ChildDriverResponse(
        Long driverId, String firstName, String lastName, String phone,
        DriverVerificationStatus verificationStatus, Instant verifiedAt) {

    public static ChildDriverResponse from(DriverProfile driver) {
        return new ChildDriverResponse(driver.getId(), driver.getFirstName(), driver.getLastName(), driver.getPhone(),
                driver.getVerificationStatus(), driver.getVerifiedAt());
    }
}
