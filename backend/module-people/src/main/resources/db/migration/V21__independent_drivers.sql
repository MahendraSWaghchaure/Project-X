-- Independent (privately-owned-vehicle) drivers register before any school claims them.
ALTER TABLE driver_profiles ALTER COLUMN school_id DROP NOT NULL;
ALTER TABLE driver_profiles ADD COLUMN vehicle_id BIGINT;
