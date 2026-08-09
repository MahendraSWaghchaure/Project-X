-- module-notification owns Flyway version range V40-V49.
CREATE TABLE notification_records (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    channel VARCHAR(16) NOT NULL,
    template_key VARCHAR(64) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    status VARCHAR(24) NOT NULL,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_records_recipient ON notification_records (recipient_user_id, created_at DESC);
