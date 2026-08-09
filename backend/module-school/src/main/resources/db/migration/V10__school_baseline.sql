-- module-school owns Flyway version range V10-V19.
-- First geometry-bearing module to migrate, so it's responsible for enabling PostGIS.
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE schools (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(255) NOT NULL,
    state VARCHAR(255),
    pincode VARCHAR(16),
    contact_phone VARCHAR(32),
    contact_email VARCHAR(255),
    location geometry(Point,4326),
    geofence_radius_meters INT NOT NULL DEFAULT 300,
    dismissal_time TIME,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE school_admin_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    registration_number VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(16) NOT NULL,
    seat_capacity INT NOT NULL,
    gps_device_id VARCHAR(128) UNIQUE,
    insurance_policy_id BIGINT,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_vehicles_school_id ON vehicles (school_id);

CREATE TABLE routes (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    vehicle_id BIGINT REFERENCES vehicles (id) ON DELETE SET NULL,
    driver_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_routes_school_id ON routes (school_id);

CREATE TABLE route_stops (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
    sequence_number INT NOT NULL,
    label VARCHAR(255) NOT NULL,
    location geometry(Point,4326) NOT NULL,
    radius_meters INT NOT NULL DEFAULT 150,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_route_stops_route_id ON route_stops (route_id);
