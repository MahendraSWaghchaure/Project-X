-- Public "contact us" inquiries from the site footer, listed for the platform admin.
CREATE TABLE inquiries (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contact VARCHAR(150) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    handled_by_user_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    handled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_inquiries_status_created ON inquiries (status, created_at DESC);
