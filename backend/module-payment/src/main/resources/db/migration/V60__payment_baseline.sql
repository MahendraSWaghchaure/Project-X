-- module-payment owns Flyway version range V60-V69.
CREATE TABLE payroll_records (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL REFERENCES driver_profiles (id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    base_salary NUMERIC(12, 2) NOT NULL,
    overtime_hours NUMERIC(6, 2) NOT NULL DEFAULT 0,
    incentive_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_paid NUMERIC(12, 2),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_payroll_records_driver_period ON payroll_records (driver_id, period_start);
