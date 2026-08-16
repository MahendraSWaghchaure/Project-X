CREATE TABLE sos_events (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips (id) ON DELETE CASCADE,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    route_id BIGINT NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    triggered_by_user_id BIGINT NOT NULL REFERENCES users (id),
    note VARCHAR(500),
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    resolved_by_user_id BIGINT REFERENCES users (id)
);
CREATE INDEX idx_sos_events_trip_id ON sos_events (trip_id);
CREATE INDEX idx_sos_events_school_id_resolved_at ON sos_events (school_id, resolved_at);
