package com.projectx.tracking.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.tracking.dto.SosEventResponse;
import com.projectx.tracking.dto.TriggerSosRequest;
import com.projectx.tracking.service.SosService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SosController {

    private final SosService sosService;

    public SosController(SosService sosService) {
        this.sosService = sosService;
    }

    @PostMapping("/trips/{tripId}/sos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'DRIVER')")
    public ApiResponse<SosEventResponse> triggerSos(@PathVariable Long tripId, @Valid @RequestBody(required = false) TriggerSosRequest request) {
        String note = request != null ? request.note() : null;
        return ApiResponse.ok(sosService.trigger(tripId, note));
    }

    @GetMapping("/schools/{schoolId}/sos/active")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<List<SosEventResponse>> listActiveForSchool(@PathVariable Long schoolId) {
        return ApiResponse.ok(sosService.listActiveForSchool(schoolId));
    }

    @PostMapping("/sos/{sosEventId}/resolve")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<SosEventResponse> resolveSos(@PathVariable Long sosEventId) {
        return ApiResponse.ok(sosService.resolve(sosEventId));
    }
}
