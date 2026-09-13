package com.projectx.geo.dto;

import com.projectx.geo.domain.Region;

public record RegionResponse(Long id, Long stateId, String name) {

    public static RegionResponse from(Region region) {
        return new RegionResponse(region.getId(), region.getStateId(), region.getName());
    }
}
