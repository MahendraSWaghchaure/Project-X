CREATE TABLE late_fee_waiver_requests (
    id BIGSERIAL PRIMARY KEY,
    fee_invoice_id BIGINT NOT NULL REFERENCES fee_invoices (id) ON DELETE CASCADE,
    parent_id BIGINT NOT NULL REFERENCES parent_profiles (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    reason VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id BIGINT,
    reviewed_at TIMESTAMPTZ,
    review_note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_late_fee_waiver_requests_school_id ON late_fee_waiver_requests (school_id);
-- A given invoice can only have one open waiver request at a time - re-requesting after a
-- rejection requires the app to only permit it once the previous one is no longer PENDING, but
-- that business rule lives in the service, not the DB (repeated PENDING would be a real bug).
