-- School-configured monthly fee, one active schedule per school.
CREATE TABLE fee_schedules (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL UNIQUE REFERENCES schools (id) ON DELETE CASCADE,
    monthly_amount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One Razorpay checkout/transaction, may settle more than one fee_invoice at once (advance payment).
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parent_profiles (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL,
    discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    months_covered INT NOT NULL DEFAULT 1,
    mode VARCHAR(16),
    razorpay_order_id VARCHAR(64) NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(64),
    razorpay_signature VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    receipt_number VARCHAR(32) UNIQUE,
    -- Comma-separated fee_invoices.id list this order is for - simpler than a join table at this
    -- scale (a payment covers at most ~12 invoices) and lets verify() know what to mark paid
    -- without needing a stateful field on fee_invoices before payment actually succeeds.
    covered_invoice_ids VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_parent_id ON payments (parent_id);

-- One row per child per billing month. payment_id is set once a Payment settles it - a single
-- advance payment can set it on several months' invoices at once.
CREATE TABLE fee_invoices (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    parent_id BIGINT NOT NULL REFERENCES parent_profiles (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    billing_month DATE NOT NULL,
    base_amount NUMERIC(10, 2) NOT NULL,
    discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    late_fee_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    due_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    payment_id BIGINT REFERENCES payments (id) ON DELETE SET NULL,
    last_late_fee_accrual_date DATE,
    late_fee_waived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (child_id, billing_month)
);
CREATE INDEX idx_fee_invoices_parent_id ON fee_invoices (parent_id);
CREATE INDEX idx_fee_invoices_school_id ON fee_invoices (school_id);
CREATE INDEX idx_fee_invoices_status ON fee_invoices (status);
