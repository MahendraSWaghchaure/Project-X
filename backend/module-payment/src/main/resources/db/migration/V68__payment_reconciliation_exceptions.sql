-- Worklist of disagreements PaymentReconciliationService found between our own payments/
-- payroll_records rows and Razorpay's own fetch-order/fetch-payment/fetch-payout response for the
-- same id. See GET /platform-admin/reconciliation-exceptions.
CREATE TABLE payment_reconciliation_exceptions (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(24) NOT NULL,
    entity_id BIGINT NOT NULL,
    gateway_reference_id VARCHAR(64),
    mismatch_type VARCHAR(24) NOT NULL,
    db_status VARCHAR(32),
    gateway_status VARCHAR(32),
    db_amount NUMERIC(12, 2),
    gateway_amount NUMERIC(12, 2),
    details VARCHAR(1000) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Backs PaymentReconciliationService's "don't re-flag (and re-notify about) an already-open
-- mismatch on every daily rerun" check and the admin listing's default ordering.
CREATE INDEX idx_reconciliation_exceptions_open_lookup
    ON payment_reconciliation_exceptions (entity_type, entity_id, mismatch_type, resolved);
CREATE INDEX idx_reconciliation_exceptions_created_at ON payment_reconciliation_exceptions (created_at DESC);
