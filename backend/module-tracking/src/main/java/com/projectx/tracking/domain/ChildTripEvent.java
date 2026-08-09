package com.projectx.tracking.domain;

import com.projectx.common.events.ChildTripEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The accountability ledger: who marked which child boarded/dropped, and when. DROPPED_AT_HOME is
 * deliberately the one event type a PARENT (not the driver) is allowed to record - see
 * ChildTripEventService.
 */
@Entity
@Table(name = "child_trip_events")
public class ChildTripEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ChildTripEventType eventType;

    @Column(name = "confirmed_by_user_id", nullable = false)
    private Long confirmedByUserId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected ChildTripEvent() {
    }

    public ChildTripEvent(Long tripId, Long childId, ChildTripEventType eventType, Long confirmedByUserId) {
        this.tripId = tripId;
        this.childId = childId;
        this.eventType = eventType;
        this.confirmedByUserId = confirmedByUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getChildId() {
        return childId;
    }

    public ChildTripEventType getEventType() {
        return eventType;
    }

    public Long getConfirmedByUserId() {
        return confirmedByUserId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
