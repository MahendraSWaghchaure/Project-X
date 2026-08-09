package com.projectx.school.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The ordered path a vehicle drives. driverId is a cross-module reference to
 * module-people's DriverProfile (by id only, see Vehicle.insurancePolicyId for why).
 */
@Entity
@Table(name = "routes")
public class Route extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false)
    private String name;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(nullable = false)
    private boolean active = true;

    /** Zone-based auto-allocation: this route serves children whose home distance from the school is in [zoneMinRadiusMeters, zoneMaxRadiusMeters). Null max = unbounded outer zone. */
    @Column(name = "zone_min_radius_meters", nullable = false)
    private int zoneMinRadiusMeters = 0;

    @Column(name = "zone_max_radius_meters")
    private Integer zoneMaxRadiusMeters;

    protected Route() {
    }

    public Route(Long schoolId, String name) {
        this.schoolId = schoolId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getZoneMinRadiusMeters() {
        return zoneMinRadiusMeters;
    }

    public Integer getZoneMaxRadiusMeters() {
        return zoneMaxRadiusMeters;
    }

    public void setZone(int zoneMinRadiusMeters, Integer zoneMaxRadiusMeters) {
        this.zoneMinRadiusMeters = zoneMinRadiusMeters;
        this.zoneMaxRadiusMeters = zoneMaxRadiusMeters;
    }

    /** Does a home this far from the school fall in this route's zone? */
    public boolean coversDistance(double distanceMeters) {
        return distanceMeters >= zoneMinRadiusMeters && (zoneMaxRadiusMeters == null || distanceMeters < zoneMaxRadiusMeters);
    }
}
