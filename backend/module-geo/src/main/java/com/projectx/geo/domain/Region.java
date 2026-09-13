package com.projectx.geo.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An intra-state grouping of districts (e.g. Maharashtra's Vidarbha/Marathwada/Konkan/Western
 * Maharashtra/Khandesh) - NOT a multi-state zone. Deliberately optional: a district's regionId can
 * be null, since this kind of grouping isn't officially/consistently named for every Indian state,
 * and guessing one would be worse than leaving it unset. Filtering falls back to State -> District
 * directly wherever a state has no regions defined.
 */
@Entity
@Table(name = "geo_regions")
public class Region extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_id", nullable = false)
    private Long stateId;

    @Column(nullable = false)
    private String name;

    protected Region() {
    }

    public Region(Long stateId, String name) {
        this.stateId = stateId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Long getStateId() {
        return stateId;
    }

    public String getName() {
        return name;
    }
}
