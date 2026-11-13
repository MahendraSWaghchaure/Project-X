package com.projectx.geo.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "geo_states")
public class State extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_id", nullable = false)
    private Long countryId;

    @Column(nullable = false)
    private String name;

    /** State/UT abbreviation, e.g. "MH" - optional, not every entry needs one filled in immediately. */
    @Column(length = 8)
    private String code;

    /** Representative (state-centroid-ish, not surveyed) coordinates - powers B3's onboarding-form map fly-to. */
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    protected State() {
    }

    public State(Long countryId, String name, String code) {
        this.countryId = countryId;
        this.name = name;
        this.code = code;
    }

    public State(Long countryId, String name, String code, Double latitude, Double longitude) {
        this.countryId = countryId;
        this.name = name;
        this.code = code;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public Long getCountryId() {
        return countryId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
