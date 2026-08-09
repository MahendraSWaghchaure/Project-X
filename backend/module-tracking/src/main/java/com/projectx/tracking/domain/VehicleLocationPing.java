package com.projectx.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.locationtech.jts.geom.Point;

/** High-volume raw GPS history from Traccar. The Redis cache (see LocationCacheService) is what read paths use for "current" position - this table is the audit trail. */
@Entity
@Table(name = "vehicle_location_pings")
public class VehicleLocationPing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "heading_degrees")
    private Double headingDegrees;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected VehicleLocationPing() {
    }

    public VehicleLocationPing(Long vehicleId, Long tripId, Point location, Double speedKmh, Double headingDegrees, Instant recordedAt) {
        this.vehicleId = vehicleId;
        this.tripId = tripId;
        this.location = location;
        this.speedKmh = speedKmh;
        this.headingDegrees = headingDegrees;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public Long getTripId() {
        return tripId;
    }

    public Point getLocation() {
        return location;
    }

    public Double getSpeedKmh() {
        return speedKmh;
    }

    public Double getHeadingDegrees() {
        return headingDegrees;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
