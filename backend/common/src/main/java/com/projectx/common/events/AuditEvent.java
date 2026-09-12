package com.projectx.common.events;

import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.trace.TraceIds;
import java.time.Instant;
import org.slf4j.MDC;

/**
 * Published by any module at a high-risk mutation point (payment status changes, child PII/medical
 * edits, driver verification changes, SOS resolution, account deactivation), consumed by
 * module-audit. Carries plain values, not entities - same convention as GeofenceRaisedEvent/
 * SosRaisedEvent - so no module needs a compile dependency on module-audit to publish one.
 *
 * @param schoolId nullable - not every action is school-scoped (e.g. platform-admin actions).
 * @param entityType a short constant like "PAYMENT", "CHILD", "DRIVER_PROFILE", "SOS_EVENT" - not
 *                    an enum, so new call sites never need a change to common.
 * @param detail a short human-readable summary of what changed, not a full diff.
 * @param traceId the originating request's trace id (see TraceIdFilter) - captured here, at
 *                publish time on the request thread, because by the time AuditEventListener
 *                consumes this the transaction has already committed and, if that ever became an
 *                {@code @Async} listener instead of a same-thread AFTER_COMMIT one, MDC would no
 *                longer reflect the original request.
 */
public record AuditEvent(
        Long actorUserId, String actorEmail, String actorRole, String action, String entityType, Long entityId,
        Long schoolId, String detail, Instant occurredAt, String traceId) {

    /** Pulls actor identity from the current request's authenticated principal - the common case for every call site. */
    public static AuditEvent of(String action, String entityType, Long entityId, Long schoolId, String detail) {
        CurrentUser actor = CurrentUserContext.get();
        return new AuditEvent(actor.userId(), actor.email(), actor.role().name(), action, entityType, entityId,
                schoolId, detail, Instant.now(), MDC.get(TraceIds.MDC_KEY));
    }
}
