-- Support for ArrivalReconciler (self-healing of lost/raced arrivals).

-- Cross-instance lease: one row per background job; a single INSERT ... ON CONFLICT DO UPDATE ... WHERE
-- locked_until < now() acquires it atomically, a crashed holder's lease simply expires.
CREATE TABLE attendance_job_lease (
    name VARCHAR(64) PRIMARY KEY,
    locked_until TIMESTAMPTZ NOT NULL
);

-- The reconciler only ever looks at "recent" rows; these keep both scans index-ranged instead of sequential.
CREATE INDEX IF NOT EXISTS idx_child_trip_events_occurred_at ON child_trip_events (occurred_at);
CREATE INDEX IF NOT EXISTS idx_arrival_date_id ON arrival_verifications (arrival_date, id);
