package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.domain.SchoolConfirm;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceResult;
import com.projectx.attendance.dto.StudentDtos.StudentRequest;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.attendance.service.ArrivalReconciler;
import com.projectx.attendance.service.ArrivalService;
import com.projectx.attendance.service.SchoolTime;
import com.projectx.common.events.ChildTripEventType;
import com.projectx.common.events.ChildTripStatusChangedEvent;
import com.projectx.common.web.ApiResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** Regression tests for the school-attendance fix round: audit, reconciler, link-child back-fill, back-dating window, version, partitions. */
class SchoolAttendanceFixRoundTest extends SchoolAttendanceTestKit {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ArrivalReconciler reconciler;
    @Autowired
    ArrivalService arrivalService;

    private ArrivalReconciler.Result reconcileNow() {
        for (int i = 0; i < 60; i++) {
            ArrivalReconciler.Result r = reconciler.runOnce();
            if (!r.skipped()) {
                return r;
            }
            sleep(500); // the scheduled run (or another test) holds the lease - wait for it
        }
        throw new AssertionError("reconciler lease never became free");
    }

    private ArrivalResponse arrivalOf(Env env, Long childId, ArrivalKind kind) {
        return tripArrivals(env, env.adminToken()).stream()
                .filter(a -> a.childId().equals(childId) && a.kind() == kind).findFirst().orElse(null);
    }

    private ArrivalResponse awaitArrivalMatching(Env env, Long childId, ArrivalKind kind, java.util.function.Predicate<ArrivalResponse> p) {
        ArrivalResponse last = null;
        for (int i = 0; i < 50; i++) {
            last = arrivalOf(env, childId, kind);
            if (last != null && p.test(last)) {
                return last;
            }
            sleep(200);
        }
        throw new AssertionError("Arrival never reached the expected state, last=" + last);
    }

    // ------------------------------------------------------------------ audit

    @Test
    void bulkAttendanceWritesAuditRowsForMarkAndEdit() {
        Env env = buildEnv();
        StudentResponse s = addStudent(env, "Audited");
        LocalDate today = SchoolTime.today();

        ok(bulk(env, env.teacherToken(), today, s.id(), AttendanceStatus.PRESENT));
        assertThat(auditCount("ATTENDANCE_MARKED", env)).as("first mark audited").isEqualTo(1);

        BulkAttendanceResult same = ok(bulk(env, env.teacherToken(), today, s.id(), AttendanceStatus.PRESENT));
        assertThat(same.unchanged()).isEqualTo(1);
        assertThat(auditCount("ATTENDANCE_EDITED", env)).as("a no-op resend is not an edit").isZero();

        ok(bulk(env, env.teacherToken(), today, s.id(), AttendanceStatus.ABSENT));
        assertThat(auditCount("ATTENDANCE_EDITED", env)).as("correction audited").isEqualTo(1);
        String detail = jdbc.queryForObject("SELECT detail FROM audit_log_entries WHERE action = 'ATTENDANCE_EDITED' AND school_id = ?",
                String.class, env.schoolId());
        assertThat(detail).contains("date=" + today).contains("editedStudents=[" + s.id() + "]");
        String role = jdbc.queryForObject("SELECT actor_role FROM audit_log_entries WHERE action = 'ATTENDANCE_EDITED' AND school_id = ?",
                String.class, env.schoolId());
        assertThat(role).isEqualTo("TEACHER");
    }

    private int auditCount(String action, Env env) {
        for (int i = 0; i < 20; i++) { // the audit row is written right after commit; allow a beat
            Integer n = jdbc.queryForObject("SELECT count(*) FROM audit_log_entries WHERE action = ? AND school_id = ?",
                    Integer.class, action, env.schoolId());
            if (n != null && n > 0) {
                return n;
            }
            sleep(100);
        }
        return 0;
    }

    // ------------------------------------------------------------------ reconciler

    @Test
    void reconcilerCreatesAnArrivalForADriverEventThatLostItsHandler() {
        Env env = buildEnv();
        Long child = env.childIds().get(0);
        StudentResponse s = subscribed(env, "Lost", child);
        ok(bulk(env, env.teacherToken(), SchoolTime.today(), s.id(), AttendanceStatus.ABSENT));

        // The event row exists (driver POST committed) but the async handler never ran: insert it directly, past the grace period.
        jdbc.update("INSERT INTO child_trip_events (trip_id, child_id, event_type, confirmed_by_user_id, occurred_at) VALUES (?, ?, 'DROPPED_AT_SCHOOL', ?, ?)",
                env.tripId(), child, env.driverUserId(), Timestamp.from(Instant.now().minusSeconds(120)));
        assertThat(arrivalOf(env, child, ArrivalKind.SCHOOL_DROP)).isNull();

        ArrivalReconciler.Result result = reconcileNow();
        assertThat(result.arrivalsCreated()).isGreaterThanOrEqualTo(1);

        ArrivalResponse arrival = awaitArrival(env, child, ArrivalKind.SCHOOL_DROP);
        assertThat(arrival.studentId()).isEqualTo(s.id());
        assertThat(arrival.schoolConfirm()).as("attendance ABSENT already stored").isEqualTo(SchoolConfirm.ABSENT);
        assertThat(arrival.outcome()).isEqualTo(ArrivalOutcome.ALERT);

        // Idempotent: another pass changes nothing and does not duplicate.
        ArrivalReconciler.Result again = reconcileNow();
        assertThat(again.arrivalsCreated()).isZero();
        assertThat(tripArrivals(env, env.adminToken())).filteredOn(a -> a.childId().equals(child)).hasSize(1);
    }

    @Test
    void reconcilerResyncsSchoolConfirmWhenTheRaceLeftItBehind() {
        Env env = buildEnv();
        Long child = env.childIds().get(0);
        StudentResponse s = subscribed(env, "Raced", child);
        assertSuccess(driverEvent(env, child, ChildTripEventType.DROPPED_AT_SCHOOL));
        ArrivalResponse opened = awaitArrival(env, child, ArrivalKind.SCHOOL_DROP);
        assertThat(opened.schoolConfirm()).isEqualTo(SchoolConfirm.PENDING);

        // Reproduce the lost update: attendance is stored, but the arrival never learned about it.
        jdbc.update("INSERT INTO school_attendance (school_id, student_id, classroom_id, attendance_date, status, marked_by_user_id) VALUES (?, ?, ?, ?, 'ABSENT', ?)",
                env.schoolId(), s.id(), env.classroomId(), java.sql.Date.valueOf(SchoolTime.today()), env.driverUserId());

        ArrivalReconciler.Result r = reconcileNow();
        // Production hardening (2026-11-13): the driver event's outbox row (see arrival_outbox) is
        // still unprocessed at this point - the fast listener opened the Arrival via
        // openFromDriverEvent, not processOutboxEvent, so it never marked that row processed. The
        // reconciler's new outbox step runs first and, incidentally, re-syncs this same Arrival
        // itself (processOutboxEvent calls the same reconcileOne as the scan-based repair path) -
        // so the fix can land via either counter depending on which step gets to it first. The real
        // assertion is the healed state below, not which counter reports the work.
        assertThat(r.arrivalsRepaired() + r.outboxProcessed()).isGreaterThanOrEqualTo(1);
        ArrivalResponse healed = arrivalOf(env, child, ArrivalKind.SCHOOL_DROP);
        assertThat(healed.schoolConfirm()).isEqualTo(SchoolConfirm.ABSENT);
        assertThat(healed.outcome()).isEqualTo(ArrivalOutcome.ALERT);
    }

    @Test
    void laterDriverEventTimeMovesDriverEventAtForwardOnly() {
        Env env = buildEnv();
        Long child = env.childIds().get(0);
        assertSuccess(driverEvent(env, child, ChildTripEventType.DROPPED_AT_SCHOOL));
        ArrivalResponse a = awaitArrival(env, child, ArrivalKind.SCHOOL_DROP);

        Instant later = a.driverEventAt().plusSeconds(90);
        arrivalService.openFromDriverEvent(new ChildTripStatusChangedEvent(env.tripId(), child, env.schoolId(),
                ChildTripEventType.DROPPED_AT_SCHOOL, env.driverUserId(), later));
        assertThat(arrivalOf(env, child, ArrivalKind.SCHOOL_DROP).driverEventAt()).isEqualTo(later);

        arrivalService.openFromDriverEvent(new ChildTripStatusChangedEvent(env.tripId(), child, env.schoolId(),
                ChildTripEventType.DROPPED_AT_SCHOOL, env.driverUserId(), a.driverEventAt()));
        assertThat(arrivalOf(env, child, ArrivalKind.SCHOOL_DROP).driverEventAt()).as("never moves backwards").isEqualTo(later);
        assertThat(tripArrivals(env, env.adminToken())).filteredOn(x -> x.childId().equals(child)).hasSize(1);
    }

    @Test
    void linkingAChildLaterBackfillsStudentIdAndSchoolConfirmation() {
        Env env = buildEnv();
        Long child = env.childIds().get(0);
        // The bus drops the child BEFORE the roster knows the child is a subscriber.
        assertSuccess(driverEvent(env, child, ChildTripEventType.DROPPED_AT_SCHOOL));
        ArrivalResponse opened = awaitArrival(env, child, ArrivalKind.SCHOOL_DROP);
        assertThat(opened.studentId()).isNull();

        StudentResponse s = addStudent(env, "LinkedLater");
        ok(bulk(env, env.teacherToken(), SchoolTime.today(), s.id(), AttendanceStatus.ABSENT));
        linkChild(env, s.id(), child);

        ArrivalResponse healed = awaitArrivalMatching(env, child, ArrivalKind.SCHOOL_DROP, a -> a.studentId() != null);
        assertThat(healed.studentId()).isEqualTo(s.id());
        // ... and a later teacher edit now reaches this arrival (it could not before: no studentId).
        ok(bulk(env, env.teacherToken(), SchoolTime.today(), s.id(), AttendanceStatus.PRESENT));
        ArrivalResponse afterEdit = awaitArrivalMatching(env, child, ArrivalKind.SCHOOL_DROP, a -> a.schoolConfirm() == SchoolConfirm.PRESENT);
        assertThat(afterEdit.outcome()).isNotEqualTo(ArrivalOutcome.ALERT);
    }

    // ------------------------------------------------------------------ input hygiene

    @Test
    void bulkDatesBeforeTheRoleWindowAreRejectedButAdminsGetALongerAuditedWindow() {
        Env env = buildEnv();
        StudentResponse s = addStudent(env, "OldDate");
        LocalDate today = SchoolTime.today();

        assertStatus(bulk(env, env.teacherToken(), today.minusDays(32), s.id(), AttendanceStatus.PRESENT), HttpStatus.BAD_REQUEST);
        assertSuccess(bulk(env, env.teacherToken(), today.minusDays(31), s.id(), AttendanceStatus.PRESENT));
        assertStatus(bulk(env, env.adminToken(), today.minusDays(401), s.id(), AttendanceStatus.PRESENT), HttpStatus.BAD_REQUEST);
        assertSuccess(bulk(env, env.adminToken(), today.minusDays(90), s.id(), AttendanceStatus.PRESENT));
        assertThat(auditCount("ATTENDANCE_MARKED", env)).isGreaterThanOrEqualTo(2);
    }

    @Test
    void updatingAStudentWithoutAVersionIsABadRequestNotLastWriteWins() {
        Env env = buildEnv();
        StudentResponse s = addStudent(env, "NoVersion");
        var noVersion = new StudentRequest(env.classroomId(), null, "NoVersion", "Changed", null, null, "G", PHONE, null, false, null, null);
        ResponseEntity<ApiResponse<StudentResponse>> r = exchange("/schools/" + env.schoolId() + "/students/" + s.id(), HttpMethod.PUT,
                noVersion, env.adminToken(), new ParameterizedTypeReference<>() {});
        assertStatus(r, HttpStatus.BAD_REQUEST);
        StudentResponse stored = ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + s.id(), HttpMethod.GET,
                null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(stored.lastName()).isEqualTo("Student");
    }

    // ------------------------------------------------------------------ partitions

    @Test
    void rowsStrandedInTheDefaultPartitionDoNotBlockPartitionCreation() {
        jdbc.execute("DROP TABLE IF EXISTS school_attendance_y2099m05");
        jdbc.update("DELETE FROM school_attendance WHERE attendance_date = DATE '2099-05-10'");
        try {
            jdbc.update("INSERT INTO school_attendance (school_id, student_id, classroom_id, attendance_date, status, marked_by_user_id) VALUES (1, 987654321, 1, DATE '2099-05-10', 'PRESENT', 1)");
            assertThat(jdbc.queryForObject("SELECT tableoid::regclass::text FROM school_attendance WHERE student_id = 987654321", String.class))
                    .isEqualTo("school_attendance_default");

            jdbc.execute("SELECT ensure_school_attendance_partition(DATE '2099-05-01')"); // used to fail: "updated partition constraint for default partition would be violated"

            assertThat(jdbc.queryForObject("SELECT tableoid::regclass::text FROM school_attendance WHERE student_id = 987654321", String.class))
                    .isEqualTo("school_attendance_y2099m05");
            jdbc.execute("SELECT ensure_school_attendance_partition(DATE '2099-05-01')"); // idempotent
        } finally {
            jdbc.execute("DROP TABLE IF EXISTS school_attendance_y2099m05");
            jdbc.update("DELETE FROM school_attendance WHERE student_id = 987654321");
        }
    }

}
