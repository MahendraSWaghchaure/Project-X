package com.projectx.notification.service;

import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.DriverVerificationStatus;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.school.repo.SchoolAdminProfileRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily sweep over {@code driver_profiles.license_expiry}. Two outcomes, same query:
 * <ul>
 *   <li><b>Already expired</b>: flips {@code verificationStatus} to {@link DriverVerificationStatus#LICENSE_EXPIRED}
 *       (if not already) - this blocks starting a new trip, see module-tracking's {@code TripService.startTrip}
 *       - and notifies the driver and their school's admins once (the status-flip check makes this a one-shot
 *       notification, not a daily repeat, for this half of the job).</li>
 *   <li><b>Expiring within the reminder window</b> (not yet expired): sends a reminder to the driver and the
 *       school's admins. No dedup/one-shot tracking for this half - a daily "N days left" nudge for an
 *       approaching deadline is the intended behavior, not a single alert that's easy to miss.</li>
 * </ul>
 * Independent drivers ({@code schoolId == null}) have no school admin to notify - driver-only in that case.
 * Terminated drivers are skipped entirely (no point blocking trips they're already barred from starting, and no
 * point nagging them to renew). Reuses {@link NotificationDispatchService} - no new dispatch mechanism.
 */
@Component
public class DriverLicenseExpiryReminderJob {

    private static final Logger log = LoggerFactory.getLogger(DriverLicenseExpiryReminderJob.class);

    private final DriverProfileRepository driverProfileRepository;
    private final SchoolAdminProfileRepository schoolAdminProfileRepository;
    private final NotificationDispatchService dispatchService;
    private final int reminderWindowDays;

    public DriverLicenseExpiryReminderJob(DriverProfileRepository driverProfileRepository,
                                           SchoolAdminProfileRepository schoolAdminProfileRepository,
                                           NotificationDispatchService dispatchService,
                                           @Value("${projectx.people.driver-license.reminder-window-days:30}") int reminderWindowDays) {
        this.driverProfileRepository = driverProfileRepository;
        this.schoolAdminProfileRepository = schoolAdminProfileRepository;
        this.dispatchService = dispatchService;
        this.reminderWindowDays = reminderWindowDays;
    }

    /** @return {newlyBlocked, remindersSent} - exposed (not void) so a test can assert on counts without re-querying. */
    public record Result(int newlyBlocked, int remindersSent) {
    }

    @Scheduled(cron = "${projectx.people.driver-license.reminder-cron:0 0 4 * * *}")
    public void scheduledRun() {
        try {
            Result r = runOnce();
            if (r.newlyBlocked() > 0 || r.remindersSent() > 0) {
                log.info("Driver license sweep: {} newly blocked (expired), {} reminders sent", r.newlyBlocked(), r.remindersSent());
            }
        } catch (Exception e) {
            log.error("Driver license expiry sweep failed", e);
        }
    }

    @Transactional
    public Result runOnce() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(reminderWindowDays);
        List<DriverProfile> candidates = driverProfileRepository.findByLicenseExpiryIsNotNullAndLicenseExpiryLessThanEqual(cutoff);
        int blocked = 0;
        int reminded = 0;
        for (DriverProfile driver : candidates) {
            if (driver.getEmploymentStatus() == DriverEmploymentStatus.TERMINATED) {
                continue;
            }
            if (driver.getLicenseExpiry().isBefore(today)) {
                if (driver.getVerificationStatus() != DriverVerificationStatus.LICENSE_EXPIRED) {
                    driver.updateVerification(DriverVerificationStatus.LICENSE_EXPIRED, null);
                    notifyExpired(driver);
                    blocked++;
                }
            } else {
                notifyExpiring(driver, today);
                reminded++;
            }
        }
        return new Result(blocked, reminded);
    }

    private void notifyExpired(DriverProfile driver) {
        String driverBody = "Your driving license " + driver.getLicenseNumber() + " expired on " + driver.getLicenseExpiry()
                + ". You will not be able to start new trips until this is resolved with your school.";
        dispatchService.dispatch(driver.getUserId(), "driver.license.expired", "Driving license expired", driverBody);
        if (driver.getSchoolId() != null) {
            String adminBody = driver.getFirstName() + " " + driver.getLastName() + "'s license (" + driver.getLicenseNumber()
                    + ") expired on " + driver.getLicenseExpiry() + " - they are now blocked from starting new trips.";
            notifySchoolAdmins(driver.getSchoolId(), "driver.license.expired", "Driver license expired", adminBody);
        }
    }

    private void notifyExpiring(DriverProfile driver, LocalDate today) {
        long daysLeft = ChronoUnit.DAYS.between(today, driver.getLicenseExpiry());
        String driverBody = "Your driving license " + driver.getLicenseNumber() + " expires on " + driver.getLicenseExpiry()
                + " (" + daysLeft + " day(s) left). Please renew it soon.";
        dispatchService.dispatch(driver.getUserId(), "driver.license.expiring", "Driving license expiring soon", driverBody);
        if (driver.getSchoolId() != null) {
            String adminBody = driver.getFirstName() + " " + driver.getLastName() + "'s license (" + driver.getLicenseNumber()
                    + ") expires on " + driver.getLicenseExpiry() + " (" + daysLeft + " day(s) left).";
            notifySchoolAdmins(driver.getSchoolId(), "driver.license.expiring", "Driver license expiring soon", adminBody);
        }
    }

    private void notifySchoolAdmins(Long schoolId, String templateKey, String subject, String body) {
        schoolAdminProfileRepository.findBySchoolId(schoolId)
                .forEach(admin -> dispatchService.dispatch(admin.getUserId(), templateKey, subject, body));
    }
}
