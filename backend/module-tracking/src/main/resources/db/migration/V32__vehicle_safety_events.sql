CREATE TABLE vehicle_safety_events (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    driver_id BIGINT NOT NULL REFERENCES driver_profiles (id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL,
    speed_kmh DOUBLE PRECISION,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_vehicle_safety_events_driver_id ON vehicle_safety_events (driver_id);
CREATE INDEX idx_vehicle_safety_events_trip_id_type ON vehicle_safety_events (trip_id, type);
