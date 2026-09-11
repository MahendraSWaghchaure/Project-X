-- module-audit owns Flyway version range V80-V89.
CREATE TABLE audit_log_entries (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    actor_email VARCHAR(255),
    actor_role VARCHAR(32),
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT,
    school_id BIGINT,
    detail VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_entries_school_id_occurred_at ON audit_log_entries (school_id, occurred_at DESC);
CREATE INDEX idx_audit_log_entries_entity_type_entity_id ON audit_log_entries (entity_type, entity_id);
