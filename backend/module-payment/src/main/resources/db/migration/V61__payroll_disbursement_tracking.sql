-- Adds real disbursement tracking to the existing payroll_records table (previously data-model-only).
ALTER TABLE payroll_records ADD COLUMN transaction_reference VARCHAR(64);
ALTER TABLE payroll_records ADD COLUMN disbursed_at TIMESTAMPTZ;
ALTER TABLE payroll_records ADD COLUMN failure_reason VARCHAR(500);
