package com.projectx.tracking.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.tracking.dto.SafetyEventResponse;
import com.projectx.tracking.service.SafetyEventQueryService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SafetyEventController {

    private final SafetyEventQueryService safetyEventQueryService;

    public SafetyEventController(SafetyEventQueryService safetyEventQueryService) {
        this.safetyEventQueryService = safetyEventQueryService;
    }

    @GetMapping("/schools/{schoolId}/drivers/{driverId}/safety-events")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<List<SafetyEventResponse>> listForDriver(@PathVariable Long schoolId, @PathVariable Long driverId) {
        return ApiResponse.ok(safetyEventQueryService.listForDriver(schoolId, driverId));
    }
}
