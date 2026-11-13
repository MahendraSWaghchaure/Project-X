-- module-attendance owns Flyway version range V100-V109.

-- ---------------------------------------------------------------------------------------------
-- Classrooms and the school roster. A student with child_id set is a Sunju transport subscriber
-- (linked to a children row); child_id NULL = non-subscribed (attendance/records only).
-- ---------------------------------------------------------------------------------------------
CREATE TABLE classrooms (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    name VARCHAR(64) NOT NULL,
    section VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_classrooms_school_name_section ON classrooms (school_id, lower(name), coalesce(lower(section), ''));

-- Sequence with INCREMENT 50 (matches the entity's allocationSize) so Hibernate can JDBC-batch the
-- inserts of POST /students/import - IDENTITY columns silently disable insert batching.
CREATE SEQUENCE students_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE students (
    id BIGINT PRIMARY KEY DEFAULT nextval('students_seq'),
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    classroom_id BIGINT REFERENCES classrooms (id) ON DELETE SET NULL,
    roll_no VARCHAR(32),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(16),
    guardian_name VARCHAR(200) NOT NULL,
    guardian_phone VARCHAR(32) NOT NULL,
    guardian_email VARCHAR(255),
    -- DPDP: students are minors. Default FALSE; only consented non-subscribed contacts are ever exposed as leads.
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    lead_source VARCHAR(64),
    child_id BIGINT REFERENCES children (id) ON DELETE SET NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_students_child_id UNIQUE (child_id)
);
CREATE INDEX idx_students_school_classroom ON students (school_id, classroom_id);
CREATE UNIQUE INDEX uq_students_classroom_roll ON students (school_id, classroom_id, roll_no)
    WHERE roll_no IS NOT NULL AND classroom_id IS NOT NULL;
-- Backs the platform-admin leads listing: a small partial index instead of scanning the roster of every school.
CREATE INDEX idx_students_leads ON students (id) WHERE marketing_consent = TRUE AND child_id IS NULL;

-- ---------------------------------------------------------------------------------------------
-- Daily attendance - the highest-volume table of this module (students x school days).
-- RANGE-partitioned by month on attendance_date. Postgres requires every unique constraint on a
-- partitioned table to contain the partition key, so the PK is (id, attendance_date) and the
-- idempotency key is UNIQUE (student_id, attendance_date). No FKs: cheap partition DETACH/DROP and
-- no per-row FK checks on the hot path; integrity is enforced by the join in the bulk upsert.
-- Not mapped as a JPA entity (all access is set-based SQL via JdbcTemplate), so Hibernate's
-- schema validation is unaffected by the partitioning.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE school_attendance (
    id BIGSERIAL NOT NULL,
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    classroom_id BIGINT,
    attendance_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL,
    marked_by_user_id BIGINT NOT NULL,
    marked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, attendance_date),
    CONSTRAINT uq_school_attendance_student_date UNIQUE (student_id, attendance_date),
    CONSTRAINT ck_school_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'LATE'))
) PARTITION BY RANGE (attendance_date);

CREATE TABLE school_attendance_default PARTITION OF school_attendance DEFAULT;

CREATE INDEX idx_school_attendance_school_date ON school_attendance (school_id, attendance_date);
CREATE INDEX idx_school_attendance_student_date ON school_attendance (student_id, attendance_date);

-- Idempotent partition creator, also called daily by AttendancePartitionJob. Creating a month that
-- already has rows sitting in the DEFAULT partition fails (Postgres refuses) - the job logs that
-- instead of crashing.
CREATE OR REPLACE FUNCTION ensure_school_attendance_partitions(months_back INT, months_ahead INT) RETURNS VOID AS $fn$
DECLARE
    month_start DATE := date_trunc('month', now() - make_interval(months => months_back))::date;
    last_start DATE := date_trunc('month', now() + make_interval(months => months_ahead))::date;
    partition_name TEXT;
BEGIN
    WHILE month_start <= last_start LOOP
        partition_name := 'school_attendance_y' || to_char(month_start, 'YYYY') || 'm' || to_char(month_start, 'MM');
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF school_attendance FOR VALUES FROM (%L) TO (%L)',
            partition_name, month_start, (month_start + interval '1 month')::date);
        month_start := (month_start + interval '1 month')::date;
    END LOOP;
END;
$fn$ LANGUAGE plpgsql;

SELECT ensure_school_attendance_partitions(2, 12);

-- ---------------------------------------------------------------------------------------------
-- Safe-arrival verification: one row per (trip, child, kind), opened by a driver event.
-- trip_id/child_id/student_id are cross-module id references (no FK), like the rest of the schema.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE arrival_verifications (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    student_id BIGINT,
    child_name VARCHAR(220) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    arrival_date DATE NOT NULL,
    driver_event_at TIMESTAMPTZ NOT NULL,
    school_confirm VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    parent_response VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    parent_note VARCHAR(500),
    parent_responded_at TIMESTAMPTZ,
    outcome VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_arrival_trip_child_kind UNIQUE (trip_id, child_id, kind)
);
CREATE INDEX idx_arrival_school_date_event ON arrival_verifications (school_id, arrival_date, driver_event_at DESC);
CREATE INDEX idx_arrival_school_date_outcome ON arrival_verifications (school_id, arrival_date, outcome);
CREATE INDEX idx_arrival_student_date ON arrival_verifications (student_id, arrival_date);
CREATE INDEX idx_arrival_child_pending ON arrival_verifications (child_id, driver_event_at DESC) WHERE parent_response = 'PENDING';

-- ---------------------------------------------------------------------------------------------
-- Basic academic records
-- ---------------------------------------------------------------------------------------------
CREATE TABLE assessments (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    exam_name VARCHAR(120) NOT NULL,
    subject VARCHAR(80) NOT NULL,
    max_marks NUMERIC(7, 2) NOT NULL,
    marks_obtained NUMERIC(7, 2) NOT NULL,
    exam_date DATE NOT NULL,
    recorded_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_assessments_marks CHECK (max_marks > 0 AND marks_obtained >= 0 AND marks_obtained <= max_marks)
);
CREATE INDEX idx_assessments_student_date ON assessments (student_id, exam_date DESC, id DESC);

CREATE TABLE student_comments (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    text VARCHAR(2000) NOT NULL,
    author_user_id BIGINT NOT NULL,
    author_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_student_comments_student ON student_comments (student_id, created_at DESC, id DESC);
