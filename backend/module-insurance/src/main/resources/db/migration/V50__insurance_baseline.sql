-- module-insurance owns Flyway version range V50-V59.
CREATE TABLE insurance_policies (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    subject_type VARCHAR(16) NOT NULL,
    subject_id BIGINT NOT NULL,
    provider VARCHAR(255) NOT NULL,
    policy_number VARCHAR(128) NOT NULL UNIQUE,
    coverage_amount NUMERIC(14, 2),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_insurance_policies_school_id ON insurance_policies (school_id);
CREATE INDEX idx_insurance_policies_subject ON insurance_policies (subject_type, subject_id);
