package com.projectx.tracking.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.tracking.cache.CachedLocation;
import com.projectx.tracking.service.LiveLocationQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** REST fallback for the initial map render; live updates after that come over the /topic/vehicles/{id}/location STOMP channel. */
@RestController
public class LiveLocationController {

    private final LiveLocationQueryService liveLocationQueryService;

    public LiveLocationController(LiveLocationQueryService liveLocationQueryService) {
        this.liveLocationQueryService = liveLocationQueryService;
    }

    @GetMapping("/schools/{schoolId}/vehicles/{vehicleId}/location")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'DRIVER')")
    public ApiResponse<CachedLocation> getVehicleLocation(@PathVariable Long schoolId, @PathVariable Long vehicleId) {
        return ApiResponse.ok(liveLocationQueryService.getVehicleLocation(schoolId, vehicleId));
    }

    @GetMapping("/parents/me/children/{childId}/vehicle-location")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<CachedLocation> getMyChildVehicleLocation(@PathVariable Long childId) {
        return ApiResponse.ok(liveLocationQueryService.getMyChildVehicleLocation(childId));
    }
}
