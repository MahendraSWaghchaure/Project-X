package com.projectx.audit.dto;

import com.projectx.audit.domain.AuditLogEntry;
import java.time.Instant;

public record AuditLogEntryResponse(
        Long id, Long actorUserId, String actorEmail, String actorRole, String action, String entityType,
        Long entityId, Long schoolId, String detail, Instant occurredAt) {

    public static AuditLogEntryResponse from(AuditLogEntry entry) {
        return new AuditLogEntryResponse(entry.getId(), entry.getActorUserId(), entry.getActorEmail(),
                entry.getActorRole(), entry.getAction(), entry.getEntityType(), entry.getEntityId(),
                entry.getSchoolId(), entry.getDetail(), entry.getOccurredAt());
    }
}
