-- module-grievance owns Flyway version range V70-V79.
-- Phase 2: structured complaint type (+ free text for OTHER), optional links to the vehicle / driver / child / trip the
-- complaint is about, and a status timeline. All link columns are cross-module id references (no FK), like the rest.
ALTER TABLE grievances ADD COLUMN type VARCHAR(32);
ALTER TABLE grievances ADD COLUMN other_description VARCHAR(500);
ALTER TABLE grievances ADD COLUMN vehicle_id BIGINT;
ALTER TABLE grievances ADD COLUMN driver_id BIGINT;
ALTER TABLE grievances ADD COLUMN child_id BIGINT;
ALTER TABLE grievances ADD COLUMN trip_id BIGINT;

-- Rows raised before types existed: derive the type from the legacy category.
UPDATE grievances SET type = CASE category
    WHEN 'SAFETY' THEN 'SAFETY_CONCERN'
    WHEN 'VEHICLE_CONDITION' THEN 'VEHICLE_CONDITION'
    WHEN 'DRIVER_CONDUCT' THEN 'DRIVER_BEHAVIOR'
    WHEN 'PAYMENT' THEN 'PAYMENT_ISSUE'
    WHEN 'DELAY' THEN 'ROUTE_DELAY'
    ELSE 'OTHER' END;
ALTER TABLE grievances ALTER COLUMN type SET NOT NULL;

CREATE INDEX idx_grievances_type ON grievances (type);
CREATE INDEX idx_grievances_school_created ON grievances (school_id, created_at DESC);
CREATE INDEX idx_grievances_created ON grievances (created_at DESC);

CREATE TABLE grievance_events (
    id BIGSERIAL PRIMARY KEY,
    grievance_id BIGINT NOT NULL REFERENCES grievances (id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL,
    note VARCHAR(2000),
    actor_user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_grievance_events_grievance ON grievance_events (grievance_id, created_at);

-- Backfill one OPEN event per existing grievance so every timeline starts at its creation.
INSERT INTO grievance_events (grievance_id, status, note, actor_user_id, created_at)
SELECT id, 'OPEN', NULL, raised_by_user_id, created_at FROM grievances;
INSERT INTO grievance_events (grievance_id, status, note, actor_user_id, created_at)
SELECT id, status, resolution_note, resolved_by_user_id, COALESCE(resolved_at, updated_at) FROM grievances WHERE status <> 'OPEN';
