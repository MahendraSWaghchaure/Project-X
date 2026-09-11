package com.projectx.audit.web;

import com.projectx.audit.dto.AuditLogEntryResponse;
import com.projectx.audit.service.AuditLogService;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/schools/{schoolId}/audit-log")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<PageResponse<AuditLogEntryResponse>> listForSchool(
            @PathVariable Long schoolId, @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(auditLogService.listForSchool(schoolId, pageable));
    }

    @GetMapping("/audit-log")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PageResponse<AuditLogEntryResponse>> listAll(@PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(auditLogService.listAll(pageable));
    }
}
