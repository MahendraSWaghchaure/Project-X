package com.projectx.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Outbox row for the arrival-opening pipeline: inserted by {@code ChildTripEventService} in the
 * SAME transaction as the {@link ChildTripEvent} it mirrors, so "this event needs an Arrival
 * opened" is durable the instant the event itself is durable - no window for loss between the two
 * writes. module-attendance's {@code ArrivalReconciler} polls unprocessed rows ({@code processedAt
 * IS NULL}) and marks them processed (via a raw SQL update, same pattern the reconciler already
 * uses for every other cross-module table it reads/writes) in the same transaction it
 * creates/advances the Arrival. module-tracking has no idea module-attendance exists - it just
 * records "this trip event happened"; the consumer decides what (if anything) to do with it.
 */
@Entity
@Table(name = "arrival_outbox")
public class ArrivalOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_event_id", nullable = false, unique = true)
    private Long tripEventId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "event_type", nullable = false, length = 24)
    private String eventType;

    @Column(name = "confirmed_by_user_id", nullable = false)
    private Long confirmedByUserId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    protected ArrivalOutbox() {
    }

    public ArrivalOutbox(Long tripEventId, Long tripId, Long childId, Long schoolId, String eventType,
                          Long confirmedByUserId, Instant occurredAt) {
        this.tripEventId = tripEventId;
        this.tripId = tripId;
        this.childId = childId;
        this.schoolId = schoolId;
        this.eventType = eventType;
        this.confirmedByUserId = confirmedByUserId;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTripEventId() {
        return tripEventId;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getChildId() {
        return childId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getConfirmedByUserId() {
        return confirmedByUserId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
