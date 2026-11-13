package com.projectx.school.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.school.domain.SchoolStatus;
import com.projectx.school.dto.SchoolResponse;
import com.projectx.school.dto.SchoolStatusCounts;
import com.projectx.school.service.SchoolService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform-admin schools list: tabs (status filter), search and paging. The un-paged {@code GET /schools} stays for existing pickers. */
@RestController
@RequestMapping("/platform-admin/schools")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformSchoolController {

    private final SchoolService schoolService;

    public PlatformSchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SchoolResponse>> list(
            @RequestParam(required = false) SchoolStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<Long> districtIds,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(schoolService.searchSchools(status, q, districtIds, page, size));
    }

    @GetMapping("/status-counts")
    public ApiResponse<SchoolStatusCounts> statusCounts() {
        return ApiResponse.ok(schoolService.statusCounts());
    }
}
