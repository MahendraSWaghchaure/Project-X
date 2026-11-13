-- module-school owns Flyway version range V10-V19.
-- Phase 2 admin UX: suspension reason on schools, vehicle owner details, scheduled time on route stops.
ALTER TABLE schools ADD COLUMN status_reason VARCHAR(500);
ALTER TABLE schools ADD COLUMN status_changed_at TIMESTAMPTZ;
CREATE INDEX idx_schools_status ON schools (status);

ALTER TABLE vehicles ADD COLUMN owner_name VARCHAR(255);
ALTER TABLE vehicles ADD COLUMN owner_phone VARCHAR(32);
ALTER TABLE vehicles ADD COLUMN owner_address VARCHAR(500);
ALTER TABLE vehicles ADD COLUMN ownership_type VARCHAR(16) NOT NULL DEFAULT 'SCHOOL';
-- Backs the vehicle -> route-name lookup of the vehicles table and the "route of this vehicle" student count.
CREATE INDEX idx_routes_vehicle_id ON routes (vehicle_id);

ALTER TABLE route_stops ADD COLUMN scheduled_time TIME;
