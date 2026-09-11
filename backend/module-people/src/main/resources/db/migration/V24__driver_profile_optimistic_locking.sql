-- Backs @Version on DriverProfile - guards concurrent edits (employment status, verification
-- status, profile details) from silently losing one.
ALTER TABLE driver_profiles ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
