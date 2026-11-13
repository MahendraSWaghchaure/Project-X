package com.projectx.school.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "route_stops")
public class RouteStop extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "radius_meters", nullable = false)
    private int radiusMeters = 150;

    @Column(name = "scheduled_time")
    private java.time.LocalTime scheduledTime;

    protected RouteStop() {
    }

    public RouteStop(Long routeId, int sequenceNumber, String label, Point location, int radiusMeters) {
        this.routeId = routeId;
        this.sequenceNumber = sequenceNumber;
        this.label = label;
        this.location = location;
        this.radiusMeters = radiusMeters;
    }

    public Long getId() {
        return id;
    }

    public Long getRouteId() {
        return routeId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getLabel() {
        return label;
    }

    public Point getLocation() {
        return location;
    }

    public int getRadiusMeters() {
        return radiusMeters;
    }

    public java.time.LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void applyDetails(int sequenceNumber, String label, Point location, int radiusMeters, java.time.LocalTime scheduledTime) {
        this.sequenceNumber = sequenceNumber;
        this.label = label;
        this.location = location;
        this.radiusMeters = radiusMeters;
        this.scheduledTime = scheduledTime;
    }

    public void setScheduledTime(java.time.LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
}
