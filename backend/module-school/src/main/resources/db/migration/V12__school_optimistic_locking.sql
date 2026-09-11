-- Backs @Version on School - guards concurrent admin edits from silently losing one.
ALTER TABLE schools ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
