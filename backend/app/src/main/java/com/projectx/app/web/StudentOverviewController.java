package com.projectx.app.web;

import com.projectx.app.dto.StudentOverviewResponse;
import com.projectx.app.service.StudentOverviewService;
import com.projectx.common.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentOverviewController {

    private final StudentOverviewService overviewService;

    public StudentOverviewController(StudentOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    /** Student detail page in one call: roster row (+consent), transport, read-only insurance, attendance history. */
    @GetMapping("/schools/{schoolId}/students/{studentId}/overview")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ApiResponse<StudentOverviewResponse> overview(@PathVariable Long schoolId, @PathVariable Long studentId,
                                                         @RequestParam(required = false) Integer days) {
        return ApiResponse.ok(overviewService.overview(schoolId, studentId, days));
    }
}
