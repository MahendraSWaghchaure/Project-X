package com.projectx.attendance.service;

import com.projectx.common.events.ChildTripStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The two seams of the arrival pipeline.
 *
 * <p>1) module-tracking publishes {@link ChildTripStatusChangedEvent} (defined in common) inside the
 * driver's transaction; module-tracking has no idea this module exists. AFTER_COMMIT means we only
 * react to events that really persisted. The handler is {@code @Async}: the request thread still
 * holds its DB connection while AFTER_COMMIT callbacks run, so doing our own REQUIRES_NEW-style
 * write inline would need a second connection per in-flight driver request - a pool-exhaustion
 * hazard under load. Hopping to attendanceExecutor releases the first connection first; the new
 * Arrival then reaches the driver/school/parent live via STOMP (/topic/trip/{id}/arrivals).
 *
 * <p>2) Our own {@link ArrivalChangedEvent}: evict the cached summary, then fan out.
 */
@Component
public class ArrivalEventListeners {

    private static final Logger log = LoggerFactory.getLogger(ArrivalEventListeners.class);

    private final ArrivalService arrivalService;
    private final ArrivalNotifier arrivalNotifier;
    private final SummaryCache summaryCache;

    public ArrivalEventListeners(ArrivalService arrivalService, ArrivalNotifier arrivalNotifier, SummaryCache summaryCache) {
        this.arrivalService = arrivalService;
        this.arrivalNotifier = arrivalNotifier;
        this.summaryCache = summaryCache;
    }

    @Async("attendanceExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDriverEvent(ChildTripStatusChangedEvent event) {
        try {
            arrivalService.openFromDriverEvent(event);
        } catch (Exception e) {
            log.error("Could not open arrival for trip {} child {} event {}", event.tripId(), event.childId(), event.type(), e);
        }
    }

    /** Async for the same reason as {@link #onDriverEvent}: an AFTER_COMMIT callback cannot start its own fresh transaction on the request thread. */
    @Async("attendanceExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudentLinked(StudentChildLinkedEvent event) {
        try {
            arrivalService.backfillLinkedChild(event.childId());
        } catch (Exception e) {
            log.error("Could not back-fill arrivals for child {} linked to student {}", event.childId(), event.studentId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArrivalChanged(ArrivalChangedEvent event) {
        summaryCache.evict(event.arrival().schoolId(), SchoolTime.today());
        arrivalNotifier.fanOut(event);
    }
}
