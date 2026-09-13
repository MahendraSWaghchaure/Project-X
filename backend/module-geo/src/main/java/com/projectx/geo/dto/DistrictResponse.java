package com.projectx.geo.dto;

/**
 * Carries its full ancestor chain (not just its own id/name) so the frontend's "search a district
 * directly, then auto-fill country/state/region" flow can populate every level from one response -
 * built in GeoService, not a static District.from(...), since that lookup needs the state/region/
 * country repositories.
 */
public record DistrictResponse(
        Long id, String name,
        Long stateId, String stateName,
        Long regionId, String regionName,
        Long countryId, String countryName) {
}
