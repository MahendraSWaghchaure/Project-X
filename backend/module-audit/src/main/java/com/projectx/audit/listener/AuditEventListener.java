package com.projectx.audit.listener;

import com.projectx.audit.domain.AuditLogEntry;
import com.projectx.audit.repo.AuditLogRepository;
import com.projectx.common.events.AuditEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * AFTER_COMMIT so a rolled-back mutation never leaves a phantom "this happened" audit row behind.
 * Deliberately no @Transactional here - the original transaction is already gone by AFTER_COMMIT,
 * and Spring disallows wrapping an AFTER_COMMIT listener in a plain (REQUIRED-propagation)
 * @Transactional for exactly that reason. auditLogRepository.save() is self-transactional via
 * Spring Data's SimpleJpaRepository, so no explicit wrapper is needed anyway.
 */
@Component
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    public AuditEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuditEvent(AuditEvent event) {
        auditLogRepository.save(new AuditLogEntry(event.actorUserId(), event.actorEmail(), event.actorRole(),
                event.action(), event.entityType(), event.entityId(), event.schoolId(), event.detail(), event.occurredAt()));
    }
}
