package com.projectx.tracking.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trips")
public class Trip extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TripDirection direction;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TripStatus status = TripStatus.SCHEDULED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected Trip() {
    }

    public Trip(Long schoolId, Long routeId, Long vehicleId, Long driverId, TripDirection direction, LocalDate tripDate) {
        this.schoolId = schoolId;
        this.routeId = routeId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.direction = direction;
        this.tripDate = tripDate;
    }

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Long getRouteId() {
        return routeId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public TripDirection getDirection() {
        return direction;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public TripStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void start() {
        if (status != TripStatus.SCHEDULED) {
            throw new IllegalStateException("Trip must be SCHEDULED to start, was " + status);
        }
        this.status = TripStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void complete() {
        if (status != TripStatus.IN_PROGRESS && status != TripStatus.DELAYED) {
            throw new IllegalStateException("Trip must be IN_PROGRESS to complete, was " + status);
        }
        this.status = TripStatus.COMPLETED;
        this.endedAt = Instant.now();
    }

    public void cancel() {
        this.status = TripStatus.CANCELLED;
        this.endedAt = Instant.now();
    }

    public void markDelayed() {
        if (status == TripStatus.IN_PROGRESS) {
            this.status = TripStatus.DELAYED;
        }
    }

    public boolean isActive() {
        return status == TripStatus.IN_PROGRESS || status == TripStatus.DELAYED;
    }
}
