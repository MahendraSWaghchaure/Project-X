-- Background-verification status a parent can see before trusting a driver with their child.
-- Existing rows default to PENDING, matching the entity default for drivers created before this
-- column existed - none of them were verified before, so PENDING is the honest starting state.
ALTER TABLE driver_profiles ADD COLUMN verification_status VARCHAR(16) NOT NULL DEFAULT 'PENDING';
ALTER TABLE driver_profiles ADD COLUMN verified_at TIMESTAMPTZ;
ALTER TABLE driver_profiles ADD COLUMN verified_by_user_id BIGINT;
