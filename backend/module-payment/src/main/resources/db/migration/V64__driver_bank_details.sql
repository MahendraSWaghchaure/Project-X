CREATE TABLE driver_bank_details (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL UNIQUE REFERENCES driver_profiles (id) ON DELETE CASCADE,
    account_holder_name VARCHAR(255) NOT NULL,
    account_number VARCHAR(64) NOT NULL,
    ifsc_code VARCHAR(16) NOT NULL,
    razorpay_contact_id VARCHAR(64),
    razorpay_fund_account_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
