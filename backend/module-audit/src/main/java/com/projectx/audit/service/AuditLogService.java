package com.projectx.audit.service;

import com.projectx.audit.dto.AuditLogEntryResponse;
import com.projectx.audit.repo.AuditLogRepository;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PageResponse<AuditLogEntryResponse> listForSchool(Long schoolId, Pageable pageable) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return PageResponse.from(auditLogRepository.findBySchoolIdOrderByOccurredAtDesc(schoolId, pageable)
                .map(AuditLogEntryResponse::from));
    }

    /** Platform-wide view, not scoped to one school - the controller gates this to PLATFORM_ADMIN only. */
    public PageResponse<AuditLogEntryResponse> listAll(Pageable pageable) {
        return PageResponse.from(auditLogRepository.findAllByOrderByOccurredAtDesc(pageable).map(AuditLogEntryResponse::from));
    }
}
