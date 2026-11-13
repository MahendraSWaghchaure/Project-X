-- Device token storage for FCM push (production hardening, 2026-11-13). One active token per user
-- (the common "latest device wins" model); the frontend re-registers on every app start/login.
CREATE TABLE push_device_tokens (
    user_id BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    device_token VARCHAR(512) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
