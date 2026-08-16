package com.projectx.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A driver-triggered panic button press, mirrored to notification via SosRaisedEvent. Stays open until a school admin resolves it. */
@Entity
@Table(name = "sos_events")
public class SosEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "triggered_by_user_id", nullable = false)
    private Long triggeredByUserId;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by_user_id")
    private Long resolvedByUserId;

    protected SosEvent() {
    }

    public SosEvent(Long tripId, Long vehicleId, Long schoolId, Long routeId, Long triggeredByUserId, String note) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.schoolId = schoolId;
        this.routeId = routeId;
        this.triggeredByUserId = triggeredByUserId;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Long getTripId() {
        return tripId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public Long getTriggeredByUserId() {
        return triggeredByUserId;
    }

    public String getNote() {
        return note;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Long getResolvedByUserId() {
        return resolvedByUserId;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    public void resolve(Long resolvedByUserId) {
        this.resolvedAt = Instant.now();
        this.resolvedByUserId = resolvedByUserId;
    }
}
