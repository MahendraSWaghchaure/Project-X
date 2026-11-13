package com.projectx.geo.dto;

/**
 * Carries its full ancestor chain (not just its own id/name) so the frontend's "search a district
 * directly, then auto-fill country/state/region" flow can populate every level from one response -
 * built in GeoService, not a static District.from(...), since that lookup needs the state/region/
 * country repositories. {@code latitude}/{@code longitude} are the district's own representative
 * coordinates (B3); {@code stateLatitude}/{@code stateLongitude} are the parent state's, used as a
 * fallback fly-to target while only country+state are picked.
 */
public record DistrictResponse(
        Long id, String name, Double latitude, Double longitude,
        Long stateId, String stateName, Double stateLatitude, Double stateLongitude,
        Long regionId, String regionName,
        Long countryId, String countryName) {
}
