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

    protected District() {
    }

    public District(Long stateId, Long regionId, String name) {
        this.stateId = stateId;
        this.regionId = regionId;
        this.name = name;
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
}
