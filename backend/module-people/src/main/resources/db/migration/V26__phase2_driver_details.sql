-- module-people owns Flyway version range V20-V29.
-- Phase 2: driver address, and why/when the employment status last changed (SUSPENDED/TERMINATED require a reason).
ALTER TABLE driver_profiles ADD COLUMN address VARCHAR(500);
ALTER TABLE driver_profiles ADD COLUMN status_reason VARCHAR(500);
ALTER TABLE driver_profiles ADD COLUMN status_changed_at TIMESTAMPTZ;
-- Backs the vehicle -> driver lookup of the vehicles table.
CREATE INDEX idx_driver_profiles_vehicle_id ON driver_profiles (vehicle_id);
-- Backs the per-vehicle student counts (children riding a route that is assigned to a vehicle).
CREATE INDEX IF NOT EXISTS idx_children_route_id ON children (route_id);
