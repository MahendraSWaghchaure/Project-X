-- module-grievance owns Flyway version range V70-V79.
CREATE TABLE grievances (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT REFERENCES schools (id) ON DELETE CASCADE,
    raised_by_user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    raised_by_role VARCHAR(16) NOT NULL,
    raised_by_email VARCHAR(255) NOT NULL,
    category VARCHAR(32) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    resolution_note VARCHAR(2000),
    resolved_by_user_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_grievances_school_id ON grievances (school_id);
CREATE INDEX idx_grievances_raised_by_user_id ON grievances (raised_by_user_id);
CREATE INDEX idx_grievances_status ON grievances (status);
