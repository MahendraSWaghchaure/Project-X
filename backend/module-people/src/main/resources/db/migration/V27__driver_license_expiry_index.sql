-- Supports DriverLicenseExpiryReminderJob's daily sweep (module-notification), which scans for
-- license_expiry <= today + reminder-window-days. Partial index (excludes NULLs - most independent
-- drivers may not have one on file yet) keeps that scan cheap regardless of table growth.
CREATE INDEX idx_driver_profiles_license_expiry ON driver_profiles (license_expiry) WHERE license_expiry IS NOT NULL;
