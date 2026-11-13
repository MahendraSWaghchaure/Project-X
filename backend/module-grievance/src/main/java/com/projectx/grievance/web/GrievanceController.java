package com.projectx.grievance.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.grievance.domain.GrievanceStatus;
import com.projectx.grievance.domain.GrievanceType;
import com.projectx.grievance.dto.CreateGrievanceRequest;
import com.projectx.grievance.dto.GrievanceResponse;
import com.projectx.grievance.dto.UpdateGrievanceStatusRequest;
import com.projectx.grievance.service.GrievanceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GrievanceController {

    private final GrievanceService grievanceService;

    public GrievanceController(GrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    @PostMapping("/grievances")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PARENT', 'DRIVER', 'SCHOOL_ADMIN')")
    public ApiResponse<GrievanceResponse> raise(@Valid @RequestBody CreateGrievanceRequest request) {
        return ApiResponse.ok(grievanceService.raise(request));
    }

    @GetMapping("/grievances/mine")
    public ApiResponse<List<GrievanceResponse>> listMine() {
        return ApiResponse.ok(grievanceService.listMine());
    }

    /** Newest 200 of the school, enriched with names. For paging/filtering use {@code /schools/{id}/grievances/search}. */
    @GetMapping("/schools/{schoolId}/grievances")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<List<GrievanceResponse>> listForSchool(@PathVariable Long schoolId) {
        return ApiResponse.ok(grievanceService.listForSchool(schoolId));
    }

    @GetMapping("/schools/{schoolId}/grievances/search")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<PageResponse<GrievanceResponse>> searchForSchool(
            @PathVariable Long schoolId,
            @RequestParam(required = false) GrievanceStatus status,
            @RequestParam(required = false) GrievanceType type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(grievanceService.search(schoolId, status, type, page, size));
    }

    /** Newest 200 across every school, enriched with names. For paging/filtering use {@code /platform-admin/grievances}. */
    @GetMapping("/grievances")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<GrievanceResponse>> listAll() {
        return ApiResponse.ok(grievanceService.listAll());
    }

    @GetMapping("/platform-admin/grievances")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PageResponse<GrievanceResponse>> searchAll(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) GrievanceStatus status,
            @RequestParam(required = false) GrievanceType type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(grievanceService.search(schoolId, status, type, page, size));
    }

    /** Full record with the status timeline: platform admin, the grievance's school admin, or whoever raised it. */
    @GetMapping("/grievances/{grievanceId}")
    public ApiResponse<GrievanceResponse> detail(@PathVariable Long grievanceId) {
        return ApiResponse.ok(grievanceService.detail(grievanceId));
    }

    @PatchMapping("/grievances/{grievanceId}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<GrievanceResponse> updateStatus(@PathVariable Long grievanceId,
                                                         @Valid @RequestBody UpdateGrievanceStatusRequest request) {
        return ApiResponse.ok(grievanceService.updateStatus(grievanceId, request));
    }
}
