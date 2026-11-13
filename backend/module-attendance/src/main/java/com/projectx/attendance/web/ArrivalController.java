package com.projectx.attendance.web;

import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.ArrivalDtos.RespondRequest;
import com.projectx.attendance.service.ArrivalService;
import com.projectx.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Driver and parent views of safe-arrival verification. Guardian checks live in ArrivalService (GuardianAccessService.isGuardian). */
@RestController
public class ArrivalController {

    private final ArrivalService arrivalService;

    public ArrivalController(ArrivalService arrivalService) {
        this.arrivalService = arrivalService;
    }

    @GetMapping("/trips/{tripId}/arrivals")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'DRIVER')")
    public ApiResponse<List<ArrivalResponse>> forTrip(@PathVariable Long tripId) {
        return ApiResponse.ok(arrivalService.listForTrip(tripId));
    }

    @GetMapping("/parents/me/arrivals/pending")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<List<ArrivalResponse>> pending() {
        return ApiResponse.ok(arrivalService.pendingForMe());
    }

    @PostMapping("/parents/me/arrivals/{arrivalId}/respond")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<ArrivalResponse> respond(@PathVariable Long arrivalId, @Valid @RequestBody RespondRequest request) {
        return ApiResponse.ok(arrivalService.respond(arrivalId, request));
    }
}
