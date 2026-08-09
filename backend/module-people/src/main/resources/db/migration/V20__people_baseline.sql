-- module-people owns Flyway version range V20-V29.
CREATE TABLE driver_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    license_number VARCHAR(64) NOT NULL UNIQUE,
    license_expiry DATE,
    insurance_policy_id BIGINT,
    base_salary_monthly NUMERIC(12, 2),
    employment_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_driver_profiles_school_id ON driver_profiles (school_id);

CREATE TABLE parent_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    invited_by_school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(32),
    address VARCHAR(500),
    home_location geometry(Point,4326),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE children (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    primary_parent_id BIGINT NOT NULL REFERENCES parent_profiles (id) ON DELETE CASCADE,
    secondary_parent_id BIGINT REFERENCES parent_profiles (id) ON DELETE SET NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    class_section VARCHAR(64),
    route_id BIGINT,
    route_stop_id BIGINT,
    insurance_policy_id BIGINT,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_children_school_id ON children (school_id);
CREATE INDEX idx_children_primary_parent_id ON children (primary_parent_id);
CREATE INDEX idx_children_route_id ON children (route_id);
