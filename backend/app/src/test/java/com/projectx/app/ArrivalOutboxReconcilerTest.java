package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.service.ArrivalReconciler;
import com.projectx.common.events.ChildTripEventType;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Production-hardening: the arrival_outbox table (module-tracking) is written in the SAME
 * transaction as child_trip_events by ChildTripEventService, and polled/processed by
 * ArrivalReconciler#processOutbox (module-attendance) - see PLAN-production-hardening.md section 1.
 * These tests call processOutbox() directly rather than waiting for the real @Scheduled trigger,
 * the same way the rest of this suite calls reconciler.runOnce() directly (see
 * SchoolAttendanceFixRoundTest) - it is the exact method the scheduler invokes, just deterministic.
 */
class ArrivalOutboxReconcilerTest extends SchoolAttendanceTestKit {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ArrivalReconciler reconciler;

    private ArrivalResponse arrivalOf(Env env, Long childId, ArrivalKind kind) {
        return tripArrivals(env, env.adminToken()).stream()
                .filter(a -> a.childId().equals(childId) && a.kind() == kind).findFirst().orElse(null);
    }

    @Test
    void outboxPollerOpensTheArrivalWhenTheAsyncListenerNeverRan() {
        Env env = buildEnv();
        Long child = env.childIds().get(0);

        // Simulate the AFTER_COMMIT listener never running at all (crash, executor rejection, etc.) by
        // inserting the event AND its durable outbox row directly - exactly what ChildTripEventService
        // would have written in one transaction - without ever publishing the application event that
        // triggers ArrivalEventListeners.onDriverEvent.
        Timestamp now = Timestamp.from(Instant.now());
        Long eventId = jdbc.queryForObject("""
                INSERT INTO child_trip_events (trip_id, child_id, event_type, confirmed_by_user_id, occurred_at)
                VALUES (?, ?, 'DROPPED_AT_SCHOOL', ?, ?) RETURNING id
                """, Long.class, env.tripId(), child, env.driverUserId(), now);
        Long outboxId = jdbc.queryForObject("""
                INSERT INTO arrival_outbox (trip_event_id, trip_id, child_id, school_id, event_type, confirmed_by_user_id, occurred_at)
                VALUES (?, ?, ?, ?, 'DROPPED_AT_SCHOOL', ?, ?) RETURNING id
                """, Long.class, eventId, env.tripId(), child, env.schoolId(), env.driverUserId(), now);

        assertThat(arrivalOf(env, child, ArrivalKind.SCHOOL_DROP)).as("nothing has processed the event yet").isNull();

        int processed = reconciler.processOutbox();
        assertThat(processed).isGreaterThanOrEqualTo(1);

        ArrivalResponse arrival = awaitArrival(env, child, ArrivalKind.SCHOOL_DROP);
        assertThat(arrival.childId()).isEqualTo(child);
        Boolean outboxMarkedProcessed = jdbc.queryForObject(
                "SELECT processed_at IS NOT NULL FROM arrival_outbox WHERE id = ?", Boolean.class, outboxId);
        assertThat(outboxMarkedProcessed).as("outbox row marked processed in the same transaction as the Arrival").isTrue();

        // Idempotent: a second poll finds nothing new (the row is no longer unprocessed) and does not duplicate.
        assertThat(reconciler.processOutbox()).isZero();
        assertThat(tripArrivals(env, env.adminToken())).filteredOn(a -> a.childId().equals(child)).hasSize(1);
    }

    @Test
    void noDuplicateArrivalWhenTheFastListenerAndTheOutboxPollerRaceOnTheSameEvent() {
        Env env = buildEnv();
        Long child = env.childIds().get(0);

        // Real path: ChildTripEventService writes child_trip_events + arrival_outbox in one transaction
        // and publishes the AFTER_COMMIT event, which schedules ArrivalEventListeners.onDriverEvent on
        // attendanceExecutor. Immediately racing the poller against that in-flight async listener.
        assertSuccess(driverEvent(env, child, ChildTripEventType.DROPPED_AT_SCHOOL));
        reconciler.processOutbox();

        // Whichever path wins, this settles to exactly one Arrival for this (trip, child, kind).
        awaitArrival(env, child, ArrivalKind.SCHOOL_DROP);
        reconciler.processOutbox(); // self-heals the loser's rolled-back transaction, if there was one

        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM arrival_verifications WHERE trip_id = ? AND child_id = ? AND kind = 'SCHOOL_DROP'",
                Integer.class, env.tripId(), child);
        assertThat(count).as("the DB unique constraint + idempotent find-or-create allow at most one").isEqualTo(1);
    }
}
