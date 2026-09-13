package com.projectx.geo.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.geo.dto.CountryResponse;
import com.projectx.geo.dto.DistrictResponse;
import com.projectx.geo.dto.RegionResponse;
import com.projectx.geo.dto.StateResponse;
import com.projectx.geo.service.GeoService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only reference-data lookups behind the admin dashboards' cascading location filters - every
 * authenticated role can read this (it's not tenant-scoped data, just a geo lookup table), so no
 * per-role @PreAuthorize beyond "is logged in at all."
 */
@RestController
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @GetMapping("/geo/countries")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CountryResponse>> listCountries() {
        return ApiResponse.ok(geoService.listCountries());
    }

    @GetMapping("/geo/states")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<StateResponse>> listStates(@RequestParam Long countryId) {
        return ApiResponse.ok(geoService.listStates(countryId));
    }

    @GetMapping("/geo/regions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<RegionResponse>> listRegions(@RequestParam Long stateId) {
        return ApiResponse.ok(geoService.listRegions(stateId));
    }

    @GetMapping("/geo/districts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<DistrictResponse>> listDistricts(
            @RequestParam(required = false) Long stateId, @RequestParam(required = false) Long regionId) {
        return ApiResponse.ok(geoService.listDistricts(stateId, regionId));
    }

    @GetMapping("/geo/districts/search")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<DistrictResponse>> searchDistricts(@RequestParam String q) {
        return ApiResponse.ok(geoService.searchDistricts(q));
    }
}
