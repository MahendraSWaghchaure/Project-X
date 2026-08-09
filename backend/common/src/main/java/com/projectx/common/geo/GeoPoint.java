package com.projectx.common.geo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/** Plain lat/lng DTO used at the API boundary; entities persist {@link org.locationtech.jts.geom.Point} instead. */
public record GeoPoint(
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude) {
}
