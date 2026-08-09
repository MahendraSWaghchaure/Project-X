package com.projectx.tracking.domain;

import com.projectx.common.events.GeofenceEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persisted record of every alert-worthy geofence crossing, mirrored to notification via GeofenceRaisedEvent. */
@Entity
@Table(name = "geofence_events")
public class GeofenceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "route_stop_id")
    private Long routeStopId;

    @Column(name = "child_id")
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private GeofenceEventType type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected GeofenceEvent() {
    }

    public GeofenceEvent(Long tripId, Long vehicleId, Long schoolId, Long routeStopId, Long childId, GeofenceEventType type) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.schoolId = schoolId;
        this.routeStopId = routeStopId;
        this.childId = childId;
        this.type = type;
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

    public Long getRouteStopId() {
        return routeStopId;
    }

    public Long getChildId() {
        return childId;
    }

    public GeofenceEventType getType() {
        return type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
