-- module-geo owns Flyway version range V90-V99.
CREATE TABLE geo_countries (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(2) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE geo_states (
    id BIGSERIAL PRIMARY KEY,
    country_id BIGINT NOT NULL REFERENCES geo_countries (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_geo_states_country_name UNIQUE (country_id, name)
);
CREATE INDEX idx_geo_states_country_id ON geo_states (country_id);

-- Intra-state grouping of districts (e.g. Maharashtra's Vidarbha/Konkan/Western Maharashtra) - see
-- Region.java's javadoc for why this is deliberately sparse rather than filled in for every state.
CREATE TABLE geo_regions (
    id BIGSERIAL PRIMARY KEY,
    state_id BIGINT NOT NULL REFERENCES geo_states (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_geo_regions_state_name UNIQUE (state_id, name)
);
CREATE INDEX idx_geo_regions_state_id ON geo_regions (state_id);

CREATE TABLE geo_districts (
    id BIGSERIAL PRIMARY KEY,
    state_id BIGINT NOT NULL REFERENCES geo_states (id) ON DELETE CASCADE,
    region_id BIGINT REFERENCES geo_regions (id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_geo_districts_state_name UNIQUE (state_id, name)
);
CREATE INDEX idx_geo_districts_state_id ON geo_districts (state_id);
CREATE INDEX idx_geo_districts_region_id ON geo_districts (region_id);
CREATE INDEX idx_geo_districts_name ON geo_districts (name);

-- Seed data: deliberately small and only what's verifiably accurate, not a fabricated full
-- nationwide dataset - see the app's own module-geo pom.xml description. More states/districts can
-- be added the same way (or via a future admin-facing write endpoint) as real need arises.

INSERT INTO geo_countries (name, code) VALUES ('India', 'IN');

INSERT INTO geo_states (country_id, name, code)
SELECT id, s.name, s.code FROM geo_countries, (VALUES
    ('Maharashtra', 'MH'),
    ('Karnataka', 'KA'),
    ('Delhi', 'DL'),
    ('Tamil Nadu', 'TN'),
    ('Gujarat', 'GJ'),
    ('Uttar Pradesh', 'UP')
) AS s(name, code)
WHERE geo_countries.code = 'IN';

-- Regions: only Maharashtra's are seeded, and only where a district below is confidently mapped to
-- one - see the districts insert's comments for which of these actually get used at seed time.
INSERT INTO geo_regions (state_id, name)
SELECT id, r.name FROM geo_states, (VALUES
    ('Vidarbha'),
    ('Konkan'),
    ('Western Maharashtra')
) AS r(name)
WHERE geo_states.name = 'Maharashtra';

INSERT INTO geo_districts (state_id, region_id, name)
SELECT s.id, NULL, d.name
FROM geo_states s, (VALUES
    ('Maharashtra', 'Nashik')
) AS d(state, name)
WHERE s.name = d.state;

INSERT INTO geo_districts (state_id, region_id, name)
SELECT s.id, r.id, d.name
FROM geo_states s
JOIN geo_regions r ON r.state_id = s.id
CROSS JOIN LATERAL (VALUES
    ('Maharashtra', 'Western Maharashtra', 'Pune'),
    ('Maharashtra', 'Konkan', 'Mumbai City'),
    ('Maharashtra', 'Konkan', 'Mumbai Suburban'),
    ('Maharashtra', 'Vidarbha', 'Nagpur')
) AS d(state, region, name)
WHERE s.name = d.state AND r.name = d.region;

INSERT INTO geo_districts (state_id, region_id, name)
SELECT s.id, NULL, d.name
FROM geo_states s, (VALUES
    ('Karnataka', 'Bengaluru Urban'),
    ('Karnataka', 'Mysuru'),
    ('Delhi', 'New Delhi'),
    ('Delhi', 'South Delhi'),
    ('Tamil Nadu', 'Chennai'),
    ('Tamil Nadu', 'Coimbatore'),
    ('Tamil Nadu', 'Madurai'),
    ('Gujarat', 'Ahmedabad'),
    ('Gujarat', 'Surat'),
    ('Gujarat', 'Vadodara'),
    ('Uttar Pradesh', 'Lucknow'),
    ('Uttar Pradesh', 'Kanpur Nagar'),
    ('Uttar Pradesh', 'Varanasi')
) AS d(state, name)
WHERE s.name = d.state;
