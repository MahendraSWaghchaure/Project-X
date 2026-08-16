-- GPS pings are the highest-volume write in this system by a wide margin (one row per vehicle per
-- ~10s during every active trip). A single unpartitioned table becomes an operational problem well
-- before it becomes a query-performance one - vacuum time, index bloat, and backup size all scale
-- with total table size regardless of how well-indexed recent data is. Converting to native
-- Postgres RANGE partitioning by month means an old month can be dropped/archived as a single fast
-- metadata operation instead of a slow row-by-row DELETE.
--
-- Postgres requires a partitioned table's primary key to include the partition column, so the PK
-- here is (id, recorded_at) rather than id alone. Nothing references this table by foreign key
-- (checked), and the application only ever queries by vehicle_id/trip_id, never a bare id, so this
-- has no functional impact.
--
-- Creating each new month's partition ahead of time is a recurring ops task (cron/manual) - not
-- automated in this pass; the DEFAULT partition below is the safety net if that ever lapses, so
-- writes never fail outright, just land somewhere unindexed-by-month until backfilled. See
-- backend/ARCHITECTURE.md.

ALTER TABLE vehicle_location_pings RENAME TO vehicle_location_pings_old;

CREATE TABLE vehicle_location_pings (
    id BIGSERIAL NOT NULL,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    trip_id BIGINT REFERENCES trips (id) ON DELETE SET NULL,
    location geometry(Point,4326) NOT NULL,
    speed_kmh DOUBLE PRECISION,
    heading_degrees DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (id, recorded_at)
) PARTITION BY RANGE (recorded_at);

CREATE TABLE vehicle_location_pings_default PARTITION OF vehicle_location_pings DEFAULT;

-- One partition per month from 3 months before today through 6 months ahead - comfortably covers
-- this dev/demo deployment without needing every possible future month pre-created.
DO $$
DECLARE
    month_start date := date_trunc('month', now() - interval '3 months');
    month_end date;
    partition_name text;
BEGIN
    FOR i IN 0..8 LOOP
        month_end := month_start + interval '1 month';
        partition_name := 'vehicle_location_pings_y' || to_char(month_start, 'YYYY') || 'm' || to_char(month_start, 'MM');
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF vehicle_location_pings FOR VALUES FROM (%L) TO (%L)',
            partition_name, month_start, month_end
        );
        month_start := month_end;
    END LOOP;
END $$;

INSERT INTO vehicle_location_pings (id, vehicle_id, trip_id, location, speed_kmh, heading_degrees, recorded_at)
    SELECT id, vehicle_id, trip_id, location, speed_kmh, heading_degrees, recorded_at FROM vehicle_location_pings_old;

SELECT setval(pg_get_serial_sequence('vehicle_location_pings', 'id'), COALESCE((SELECT MAX(id) FROM vehicle_location_pings), 1));

DROP TABLE vehicle_location_pings_old;

CREATE INDEX idx_vehicle_location_pings_vehicle_id_recorded_at ON vehicle_location_pings (vehicle_id, recorded_at DESC);
CREATE INDEX idx_vehicle_location_pings_trip_id ON vehicle_location_pings (trip_id);
-- Today's geofence checks compute distance in application code against small, id-keyed lookups
-- (one school, a handful of route stops), not a spatial SQL query, so this isn't fixing an
-- existing slow query - it's here so the next spatial query this table needs (nearest-vehicle
-- lookups, historical path queries by location) is O(log n) via GiST from day one.
CREATE INDEX idx_vehicle_location_pings_location_gist ON vehicle_location_pings USING GIST (location);
