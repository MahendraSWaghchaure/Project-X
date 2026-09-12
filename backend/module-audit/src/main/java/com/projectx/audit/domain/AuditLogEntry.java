package com.projectx.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One row per high-risk mutation - who did what, to which entity, when. Immutable once written; nothing ever updates or deletes a row here. */
@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(name = "actor_role", length = 32)
    private String actorRole;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(length = 1000)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** The HTTP request's trace id (see common.trace.TraceIdFilter) - correlates this row back to every log line written while handling that request. Nullable: a handful of audit-worthy actions today don't run inside an HTTP request (e.g. a scheduled job), so there's no trace id to capture. */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    protected AuditLogEntry() {
    }

    public AuditLogEntry(Long actorUserId, String actorEmail, String actorRole, String action, String entityType,
                          Long entityId, Long schoolId, String detail, Instant occurredAt, String traceId) {
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.schoolId = schoolId;
        this.detail = detail;
        this.occurredAt = occurredAt;
        this.traceId = traceId;
    }

    public Long getId() {
        return id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getTraceId() {
        return traceId;
    }
}
