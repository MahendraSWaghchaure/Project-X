-- app module owns Flyway version range V44-V49 (first migration owned by `app` itself, rather than
-- a feature module - see ProjectXApplication's javadoc on why app is the cross-module composition
-- root; this table has no natural single-tenant-module home). Picked an unclaimed gap between
-- module-notification's V41-43 and module-insurance's V50 - every module's range is independent
-- (see application.yml's flyway.out-of-order note), so a gap like this is safe to claim later too.
-- F1: lightweight "Partner" leads (insurance company / legal advisor / RTO agent) - status is
-- always COMING_SOON for now; see Partner.java's javadoc.
CREATE TABLE partners (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    type VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(32),
    notes VARCHAR(1000),
    status VARCHAR(16) NOT NULL DEFAULT 'COMING_SOON',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_partners_type ON partners (type);
