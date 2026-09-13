package com.projectx.geo.dto;

import com.projectx.geo.domain.State;

public record StateResponse(Long id, Long countryId, String name, String code) {

    public static StateResponse from(State state) {
        return new StateResponse(state.getId(), state.getCountryId(), state.getName(), state.getCode());
    }
}
