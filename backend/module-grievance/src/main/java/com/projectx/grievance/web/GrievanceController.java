package com.projectx.grievance.web;

import com.projectx.common.web.ApiResponse;
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

    @GetMapping("/schools/{schoolId}/grievances")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<List<GrievanceResponse>> listForSchool(@PathVariable Long schoolId) {
        return ApiResponse.ok(grievanceService.listForSchool(schoolId));
    }

    @GetMapping("/grievances")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<GrievanceResponse>> listAll() {
        return ApiResponse.ok(grievanceService.listAll());
    }

    @PatchMapping("/grievances/{grievanceId}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<GrievanceResponse> updateStatus(@PathVariable Long grievanceId,
                                                         @Valid @RequestBody UpdateGrievanceStatusRequest request) {
        return ApiResponse.ok(grievanceService.updateStatus(grievanceId, request));
    }
}
