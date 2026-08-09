package com.projectx.school.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.SchoolResponse;
import com.projectx.school.service.SchoolService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schools")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<SchoolResponse> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        return ApiResponse.ok(schoolService.createSchool(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<SchoolResponse>> listSchools() {
        return ApiResponse.ok(schoolService.listSchools());
    }

    @GetMapping("/{schoolId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<SchoolResponse> getSchool(@PathVariable Long schoolId) {
        return ApiResponse.ok(schoolService.getSchool(schoolId));
    }
}
