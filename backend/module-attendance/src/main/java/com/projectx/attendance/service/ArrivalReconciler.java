package com.projectx.attendance.service;

import com.projectx.common.events.ChildTripEventType;
import com.projectx.common.events.ChildTripStatusChangedEvent;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Self-healing for the safe-arrival pipeline, which is deliberately asynchronous and therefore lossy at
 * the edges (executor rejection, a crash between the driver's commit and the AFTER_COMMIT handler, the
 * driver-event vs bulk-attendance race). Every run, bounded and idempotent:
 * <ol>
 *   <li><b>processes the durable outbox</b> ({@code arrival_outbox}, written by module-tracking's
 *       ChildTripEventService in the SAME transaction as the child_trip_events row it mirrors) - this
 *       is now the source of truth for "an Arrival needs to exist for this event" and guarantees
 *       eventual correctness even if the AFTER_COMMIT listener never ran at all;</li>
 *   <li>creates the Arrival for every recent {@code child_trip_events} row that still has none (a second,
 *       belt-and-suspenders scan over the raw events table - catches anything that bypassed the outbox,
 *       e.g. a row inserted directly, or predates this outbox existing - using the very same code path
 *       as the live handler, so a replay can never double-open one);</li>
 *   <li>repairs recent arrivals whose studentId is missing although the child is now linked, or whose
 *       school_confirm disagrees with the stored school_attendance row.</li>
 * </ol>
 * Safe on N instances: a cross-instance lease row (single atomic upsert, self-expiring) lets exactly one
 * instance run at a time, and each unit of work is its own small transaction, so a slow run never holds a
 * connection or a lock for long. Work per run is capped ({@code batch-size} rows x {@code max-batches});
 * the remainder is picked up by the next run. Look-back, grace and cadence are configurable.
 *
 * <p><b>Polling interval (documented choice):</b> default lowered from 60s to 15s. The outbox step's
 * query is a tiny partial-index scan ({@code WHERE processed_at IS NULL}) bounded by batch-size/max-batches
 * regardless of frequency, so polling 4x more often does not add meaningful load - it just shrinks the
 * worst-case recovery window for a crash/executor-rejection scenario from up to 60s to up to 15s.
 */
@Component
public class ArrivalReconciler {

    private static final Logger log = LoggerFactory.getLogger(ArrivalReconciler.class);
    static final String LEASE_NAME = "arrival-reconciler";
    private static final int LEASE_MINUTES = 5;

    private final JdbcTemplate jdbc;
    private final ArrivalService arrivalService;
    private final int lookbackHours;
    private final int graceSeconds;
    private final int batchSize;
    private final int maxBatches;

    public ArrivalReconciler(JdbcTemplate jdbc, ArrivalService arrivalService,
                             @Value("${projectx.attendance.reconcile.lookback-hours:48}") int lookbackHours,
                             @Value("${projectx.attendance.reconcile.grace-seconds:30}") int graceSeconds,
                             @Value("${projectx.attendance.reconcile.batch-size:200}") int batchSize,
                             @Value("${projectx.attendance.reconcile.max-batches:5}") int maxBatches) {
        this.jdbc = jdbc;
        this.arrivalService = arrivalService;
        this.lookbackHours = lookbackHours;
        this.graceSeconds = graceSeconds;
        this.batchSize = batchSize;
        this.maxBatches = maxBatches;
    }

    /** @param skipped true when another instance (or an overlapping run) holds the lease. */
    public record Result(boolean skipped, int arrivalsCreated, int arrivalsRepaired, int outboxProcessed) {
    }

    @Scheduled(fixedDelayString = "${projectx.attendance.reconcile.interval-ms:15000}",
            initialDelayString = "${projectx.attendance.reconcile.initial-delay-ms:15000}")
    public void scheduledRun() {
        try {
            runOnce();
        } catch (Exception e) {
            log.error("Arrival reconciliation run failed", e);
        }
    }

    public Result runOnce() {
        if (!acquireLease()) {
            return new Result(true, 0, 0, 0);
        }
        try {
            int outboxProcessed = processOutbox();
            int created = createMissingArrivals();
            int repaired = repairOpenArrivals();
            if (created > 0 || repaired > 0 || outboxProcessed > 0) {
                log.info("Arrival reconciler processed {} outbox rows, healed {} missing and {} out-of-date arrivals",
                        outboxProcessed, created, repaired);
            }
            return new Result(false, created, repaired, outboxProcessed);
        } finally {
            releaseLease();
        }
    }

    // ------------------------------------------------------------------ steps

    private record OutboxRow(long outboxId, long tripId, long childId, long schoolId, String eventType,
                              long confirmedByUserId, Instant occurredAt) {
    }

    /**
     * The durable source of truth: every unprocessed {@code arrival_outbox} row (written by
     * module-tracking's ChildTripEventService in the same transaction as the driver event itself) is
     * replayed through the exact same idempotent open/advance logic the live AFTER_COMMIT listener uses
     * ({@code ArrivalService.processOutboxEvent}), which also marks the row processed in that same
     * transaction. Public so a test can drive it directly, independent of the other (belt-and-suspenders)
     * steps below.
     */
    public int processOutbox() {
        int total = 0;
        for (int batch = 0; batch < maxBatches; batch++) {
            List<OutboxRow> rows = jdbc.query("""
                    SELECT id, trip_id, child_id, school_id, event_type, confirmed_by_user_id, occurred_at
                      FROM arrival_outbox
                     WHERE processed_at IS NULL
                     ORDER BY id
                     LIMIT ?
                    """, (rs, i) -> new OutboxRow(rs.getLong("id"), rs.getLong("trip_id"), rs.getLong("child_id"),
                    rs.getLong("school_id"), rs.getString("event_type"), rs.getLong("confirmed_by_user_id"),
                    rs.getTimestamp("occurred_at").toInstant()), batchSize);
            int processed = 0;
            for (OutboxRow r : rows) {
                try {
                    arrivalService.processOutboxEvent(r.outboxId(), new ChildTripStatusChangedEvent(r.tripId(), r.childId(),
                            r.schoolId(), ChildTripEventType.valueOf(r.eventType()), r.confirmedByUserId(), r.occurredAt()));
                    processed++;
                } catch (Exception e) {
                    // e.g. lost an insert race with the live handler: the row is retried next pass, by which
                    // point the winner's Arrival is visible and this just marks the outbox row processed.
                    log.info("Reconciler could not process outbox row {}: {}", r.outboxId(), e.toString());
                }
            }
            total += processed;
            if (rows.size() < batchSize || processed == 0) {
                break;
            }
        }
        return total;
    }

    private record MissingEvent(long tripId, long childId, long schoolId, String type, long confirmedBy, Instant occurredAt) {
    }

    /** Driver events (recent, past the grace period) that never produced an Arrival of their kind. */
    private int createMissingArrivals() {
        Instant now = Instant.now();
        Timestamp from = Timestamp.from(now.minus(lookbackHours, ChronoUnit.HOURS));
        Timestamp until = Timestamp.from(now.minusSeconds(graceSeconds));
        int total = 0;
        for (int batch = 0; batch < maxBatches; batch++) {
            List<MissingEvent> missing = jdbc.query("""
                    SELECT x.trip_id, x.child_id, x.school_id, x.event_type, x.confirmed_by_user_id, x.occurred_at
                      FROM (SELECT e.id, e.trip_id, e.child_id, t.school_id, e.event_type, e.confirmed_by_user_id, e.occurred_at,
                                   CASE e.event_type
                                        WHEN 'BOARDED' THEN CASE WHEN t.direction = 'MORNING_PICKUP' THEN 'HOME_PICKUP' END
                                        WHEN 'DROPPED_AT_SCHOOL' THEN 'SCHOOL_DROP'
                                        WHEN 'PICKED_FROM_SCHOOL' THEN 'SCHOOL_PICKUP'
                                   END AS kind
                              FROM child_trip_events e JOIN trips t ON t.id = e.trip_id
                             WHERE e.occurred_at >= ? AND e.occurred_at < ?) x
                     WHERE x.kind IS NOT NULL
                       AND NOT EXISTS (SELECT 1 FROM arrival_verifications a
                                        WHERE a.trip_id = x.trip_id AND a.child_id = x.child_id AND a.kind = x.kind)
                     ORDER BY x.id
                     LIMIT ?
                    """, (rs, i) -> new MissingEvent(rs.getLong("trip_id"), rs.getLong("child_id"), rs.getLong("school_id"),
                    rs.getString("event_type"), rs.getLong("confirmed_by_user_id"), rs.getTimestamp("occurred_at").toInstant()),
                    from, until, batchSize);
            int created = 0;
            for (MissingEvent m : missing) {
                try {
                    arrivalService.openFromDriverEvent(new ChildTripStatusChangedEvent(m.tripId(), m.childId(), m.schoolId(),
                            ChildTripEventType.valueOf(m.type()), m.confirmedBy(), m.occurredAt()));
                    created++;
                } catch (Exception e) {
                    // e.g. lost an insert race with the live handler: harmless, the row exists now.
                    log.info("Reconciler could not open arrival for trip {} child {}: {}", m.tripId(), m.childId(), e.toString());
                }
            }
            total += created;
            if (missing.size() < batchSize || created == 0) {
                break;
            }
        }
        return total;
    }

    /** Recent arrivals with no studentId although a student is linked now, or whose school_confirm lags the stored attendance. */
    private int repairOpenArrivals() {
        java.sql.Date since = java.sql.Date.valueOf(SchoolTime.today().minusDays(2));
        int total = 0;
        for (int batch = 0; batch < maxBatches; batch++) {
            List<Long> ids = jdbc.queryForList("""
                    SELECT a.id
                      FROM arrival_verifications a
                      LEFT JOIN students st ON a.student_id IS NULL AND st.child_id = a.child_id
                      LEFT JOIN school_attendance sa ON sa.student_id = COALESCE(a.student_id, st.id) AND sa.attendance_date = a.arrival_date
                     WHERE a.arrival_date >= ?
                       AND ((a.student_id IS NULL AND st.id IS NOT NULL)
                            OR (sa.status IS NOT NULL
                                AND a.school_confirm <> CASE sa.status WHEN 'ABSENT' THEN 'ABSENT' ELSE 'PRESENT' END))
                     ORDER BY a.id
                     LIMIT ?
                    """, Long.class, since, batchSize);
            int repaired = 0;
            for (Long id : ids) {
                try {
                    if (arrivalService.reconcileOne(id)) {
                        repaired++;
                    }
                } catch (Exception e) {
                    log.info("Reconciler could not repair arrival {}: {}", id, e.toString());
                }
            }
            total += repaired;
            if (ids.size() < batchSize || repaired == 0) {
                break;
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ lease

    private boolean acquireLease() {
        return jdbc.update("""
                INSERT INTO attendance_job_lease (name, locked_until) VALUES (?, now() + make_interval(mins => ?))
                ON CONFLICT (name) DO UPDATE SET locked_until = EXCLUDED.locked_until
                 WHERE attendance_job_lease.locked_until < now()
                """, LEASE_NAME, LEASE_MINUTES) == 1;
    }

    private void releaseLease() {
        jdbc.update("UPDATE attendance_job_lease SET locked_until = now() WHERE name = ?", LEASE_NAME);
    }
}
