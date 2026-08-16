package com.projectx.tracking.domain;

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
 * A speeding or harsh-braking incident flagged from reported GPS pings - a threshold rule on
 * self-reported device speed, not validated against real accelerometer data. driverId is
 * denormalized here (not just derivable via tripId) so a per-driver safety score is a plain count
 * query, not a join through trips.
 */
@Entity
@Table(name = "vehicle_safety_events")
public class VehicleSafetyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SafetyEventType type;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected VehicleSafetyEvent() {
    }

    public VehicleSafetyEvent(Long tripId, Long vehicleId, Long schoolId, Long driverId, SafetyEventType type, Double speedKmh) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.schoolId = schoolId;
        this.driverId = driverId;
        this.type = type;
        this.speedKmh = speedKmh;
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

    public Long getDriverId() {
        return driverId;
    }

    public SafetyEventType getType() {
        return type;
    }

    public Double getSpeedKmh() {
        return speedKmh;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
