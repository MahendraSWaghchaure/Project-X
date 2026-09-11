package com.projectx.common.events;

import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import java.time.Instant;

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
 */
public record AuditEvent(
        Long actorUserId, String actorEmail, String actorRole, String action, String entityType, Long entityId,
        Long schoolId, String detail, Instant occurredAt) {

    /** Pulls actor identity from the current request's authenticated principal - the common case for every call site. */
    public static AuditEvent of(String action, String entityType, Long entityId, Long schoolId, String detail) {
        CurrentUser actor = CurrentUserContext.get();
        return new AuditEvent(actor.userId(), actor.email(), actor.role().name(), action, entityType, entityId,
                schoolId, detail, Instant.now());
    }
}
