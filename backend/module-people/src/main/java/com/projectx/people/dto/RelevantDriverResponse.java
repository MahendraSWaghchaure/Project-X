package com.projectx.people.dto;

/** E3: one driver who actually drives a route carrying one of the parent's children (deduped across children). */
public record RelevantDriverResponse(Long driverId, String name, String vehicleNo) {
}
