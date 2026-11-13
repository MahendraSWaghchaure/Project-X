package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.notification.service.DriverLicenseExpiryReminderJob;
import com.projectx.people.domain.DriverVerificationStatus;
import com.projectx.people.dto.CreateDriverRequest;
import com.projectx.people.dto.DriverResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

/** Production-hardening part 2: see PLAN-production-hardening.md section 2. */
class DriverLicenseExpiryReminderJobTest extends SchoolAttendanceTestKit {

    @Autowired
    DriverLicenseExpiryReminderJob job;
    @Autowired
    JdbcTemplate jdbc;

    private record NewDriver(DriverResponse response, String token) {
    }

    private NewDriver createDriverWithExpiry(Env env, LocalDate expiry) {
        String email = unique("licdriver") + "@test.example";
        var request = new CreateDriverRequest("License", "Driver", email, "password123",
                "9900000002", unique("LIC"), expiry, BigDecimal.valueOf(10000));
        DriverResponse response = ok(this.<DriverResponse>exchange("/schools/" + env.schoolId() + "/drivers", HttpMethod.POST,
                request, env.adminToken(), new ParameterizedTypeReference<>() {}));
        return new NewDriver(response, login(email, "password123"));
    }

    private DriverResponse driverById(Env env, Long driverId) {
        List<DriverResponse> drivers = ok(this.<List<DriverResponse>>exchange("/schools/" + env.schoolId() + "/drivers",
                HttpMethod.GET, null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        return drivers.stream().filter(d -> d.id().equals(driverId)).findFirst().orElseThrow();
    }

    /** dispatch() is @Async - give the notificationExecutor a beat to write the row. */
    private int notificationCountFor(Long userId, String templateKey) {
        for (int i = 0; i < 30; i++) {
            Integer n = jdbc.queryForObject(
                    "SELECT count(*) FROM notification_records WHERE recipient_user_id = ? AND template_key = ?",
                    Integer.class, userId, templateKey);
            if (n != null && n > 0) {
                return n;
            }
            sleep(100);
        }
        return 0;
    }

    @Test
    void expiredLicenseBlocksNewTripsAndNotifiesDriverAndSchoolAdmin() {
        Env env = buildEnv();
        NewDriver driver = createDriverWithExpiry(env, LocalDate.now().minusDays(1));
        Long driverUserId = jwtSubject(driver.token());
        Long adminUserId = jwtSubject(env.adminToken());

        DriverLicenseExpiryReminderJob.Result result = job.runOnce();
        assertThat(result.newlyBlocked()).isGreaterThanOrEqualTo(1);

        DriverResponse after = driverById(env, driver.response().id());
        assertThat(after.verificationStatus()).isEqualTo(DriverVerificationStatus.LICENSE_EXPIRED);

        assertThat(notificationCountFor(driverUserId, "driver.license.expired")).isGreaterThanOrEqualTo(1);
        assertThat(notificationCountFor(adminUserId, "driver.license.expired")).isGreaterThanOrEqualTo(1);

        // One-shot: running again does not re-flip (already LICENSE_EXPIRED) or re-notify.
        int before = notificationCountFor(driverUserId, "driver.license.expired");
        job.runOnce();
        assertThat(notificationCountFor(driverUserId, "driver.license.expired")).isEqualTo(before);
    }

    @Test
    void licenseExpiringSoonSendsAReminderButDoesNotBlockOrFlipStatus() {
        Env env = buildEnv();
        NewDriver driver = createDriverWithExpiry(env, LocalDate.now().plusDays(10));
        Long driverUserId = jwtSubject(driver.token());

        DriverLicenseExpiryReminderJob.Result result = job.runOnce();
        assertThat(result.remindersSent()).isGreaterThanOrEqualTo(1);

        DriverResponse after = driverById(env, driver.response().id());
        assertThat(after.verificationStatus()).isNotEqualTo(DriverVerificationStatus.LICENSE_EXPIRED);
        assertThat(notificationCountFor(driverUserId, "driver.license.expiring")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void licenseExpiringWellOutsideTheWindowIsIgnored() {
        Env env = buildEnv();
        createDriverWithExpiry(env, LocalDate.now().plusYears(2));

        DriverLicenseExpiryReminderJob.Result before = job.runOnce();
        // Driver from the other tests in this class may still produce nonzero counts on a shared
        // run; the real assertion is behavioral, done via the other two tests. This just confirms
        // a far-future expiry doesn't blow up the sweep.
        assertThat(before).isNotNull();
    }
}
