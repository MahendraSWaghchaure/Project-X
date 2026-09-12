package com.projectx.audit.listener;

import com.projectx.audit.domain.AuditLogEntry;
import com.projectx.audit.repo.AuditLogRepository;
import com.projectx.common.events.AuditEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * AFTER_COMMIT so a rolled-back mutation never leaves a phantom "this happened" audit row behind.
 * REQUIRES_NEW (not REQUIRED, and not left off entirely) is the one propagation Spring actually
 * allows on an AFTER_COMMIT listener - by this point the original transaction has already
 * finished, so relying on Spring Data's repository-method-level @Transactional to "just work" here
 * silently no-ops: the save() call returns without throwing, but the entity's IDENTITY-generated
 * id comes back null, because there's no genuinely fresh, still-open transaction/connection for
 * the INSERT to run in - confirmed by instrumenting this exact method during development, not
 * assumed. REQUIRES_NEW forces a real independent transaction instead of ambiguously trying to
 * participate in the one that just completed.
 */
@Component
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    public AuditEventListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditEvent(AuditEvent event) {
        auditLogRepository.save(new AuditLogEntry(event.actorUserId(), event.actorEmail(), event.actorRole(),
                event.action(), event.entityType(), event.entityId(), event.schoolId(), event.detail(),
                event.occurredAt(), event.traceId()));
    }
}
