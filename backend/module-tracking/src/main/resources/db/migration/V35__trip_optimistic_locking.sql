-- Backs @Version on Trip - guards concurrent status transitions (start/complete/cancel racing)
-- from silently losing one under real concurrent writes. Existing rows default to 0, matching a
-- freshly-loaded JPA entity's initial version.
ALTER TABLE trips ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
