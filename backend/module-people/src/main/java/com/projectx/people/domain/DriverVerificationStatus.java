package com.projectx.people.domain;

public enum DriverVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    /**
     * Set automatically by DriverLicenseExpiryReminderJob (module-notification) when a driver's
     * licenseExpiry is in the past; distinct from REJECTED (an admin's document review) since the
     * cause and the remedy differ (renew the license vs. resubmit documents). Blocks starting a
     * new trip (see TripService.startTrip); cleared back to PENDING/VERIFIED only by an explicit
     * re-verification (DriverService.updateVerificationStatus) once the license is renewed.
     */
    LICENSE_EXPIRED
}
