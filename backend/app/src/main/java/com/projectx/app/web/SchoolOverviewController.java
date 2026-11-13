package com.projectx.app.web;

import com.projectx.app.dto.SchoolOverviewResponse;
import com.projectx.app.service.SchoolOverviewService;
import com.projectx.common.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** B1: platform admin's school detail page (click a school in the list). */
@RestController
public class SchoolOverviewController {

    private final SchoolOverviewService schoolOverviewService;

    public SchoolOverviewController(SchoolOverviewService schoolOverviewService) {
        this.schoolOverviewService = schoolOverviewService;
    }

    @GetMapping("/platform-admin/schools/{id}/overview")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<SchoolOverviewResponse> overview(@PathVariable Long id) {
        return ApiResponse.ok(schoolOverviewService.overview(id));
    }
}
