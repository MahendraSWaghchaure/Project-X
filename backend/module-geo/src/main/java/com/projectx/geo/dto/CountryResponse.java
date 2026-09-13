package com.projectx.geo.dto;

import com.projectx.geo.domain.Country;

public record CountryResponse(Long id, String name, String code) {

    public static CountryResponse from(Country country) {
        return new CountryResponse(country.getId(), country.getName(), country.getCode());
    }
}
