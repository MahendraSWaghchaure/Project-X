package com.projectx.geo.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "geo_districts")
public class District extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_id", nullable = false)
    private Long stateId;

    /** Nullable - see Region's javadoc on why not every district has one. */
    @Column(name = "region_id")
    private Long regionId;

    @Column(nullable = false)
    private String name;

    /** Representative (district-HQ-town-ish, not surveyed) coordinates - powers B3's onboarding-form map fly-to. */
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    protected District() {
    }

    public District(Long stateId, Long regionId, String name) {
        this.stateId = stateId;
        this.regionId = regionId;
        this.name = name;
    }

    public District(Long stateId, Long regionId, String name, Double latitude, Double longitude) {
        this.stateId = stateId;
        this.regionId = regionId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public Long getStateId() {
        return stateId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
