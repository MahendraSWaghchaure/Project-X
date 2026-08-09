-- Independent (privately-owned) drivers register their own vehicle before any school claims them.
ALTER TABLE vehicles ALTER COLUMN school_id DROP NOT NULL;

-- Zone-based auto-allocation: a route serves children whose home distance from the school falls
-- in [zone_min_radius_meters, zone_max_radius_meters). Null max = unbounded outer zone.
ALTER TABLE routes ADD COLUMN zone_min_radius_meters INT NOT NULL DEFAULT 0;
ALTER TABLE routes ADD COLUMN zone_max_radius_meters INT;
