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

    protected State() {
    }

    public State(Long countryId, String name, String code) {
        this.countryId = countryId;
        this.name = name;
        this.code = code;
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
}
