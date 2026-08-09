-- module-tracking owns Flyway version range V30-V39.
CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    route_id BIGINT NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    driver_id BIGINT NOT NULL REFERENCES driver_profiles (id) ON DELETE CASCADE,
    direction VARCHAR(24) NOT NULL,
    trip_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_trips_school_id_trip_date ON trips (school_id, trip_date);
CREATE INDEX idx_trips_driver_id_trip_date ON trips (driver_id, trip_date);
CREATE INDEX idx_trips_vehicle_id_status ON trips (vehicle_id, status);
CREATE INDEX idx_trips_status ON trips (status);

CREATE TABLE child_trip_events (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    child_id BIGINT NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    event_type VARCHAR(24) NOT NULL,
    confirmed_by_user_id BIGINT NOT NULL REFERENCES users (id),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_child_trip_events_trip_id ON child_trip_events (trip_id);
CREATE INDEX idx_child_trip_events_child_id ON child_trip_events (child_id);
CREATE UNIQUE INDEX uq_child_trip_events_trip_child_type ON child_trip_events (trip_id, child_id, event_type);

CREATE TABLE vehicle_location_pings (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    trip_id BIGINT REFERENCES trips (id) ON DELETE SET NULL,
    location geometry(Point,4326) NOT NULL,
    speed_kmh DOUBLE PRECISION,
    heading_degrees DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_vehicle_location_pings_vehicle_id_recorded_at ON vehicle_location_pings (vehicle_id, recorded_at DESC);
CREATE INDEX idx_vehicle_location_pings_trip_id ON vehicle_location_pings (trip_id);

CREATE TABLE geofence_events (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    route_stop_id BIGINT REFERENCES route_stops (id) ON DELETE SET NULL,
    child_id BIGINT REFERENCES children (id) ON DELETE CASCADE,
    type VARCHAR(24) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_geofence_events_trip_id ON geofence_events (trip_id);
