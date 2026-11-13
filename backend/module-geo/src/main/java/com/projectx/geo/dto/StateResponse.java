package com.projectx.geo.dto;

import com.projectx.geo.domain.State;

public record StateResponse(Long id, Long countryId, String name, String code, Double latitude, Double longitude) {

    public static StateResponse from(State state) {
        return new StateResponse(state.getId(), state.getCountryId(), state.getName(), state.getCode(),
                state.getLatitude(), state.getLongitude());
    }
}
