-- module-attendance owns Flyway version range V100-V109.
-- Phase 2: teacher profiles, class teacher per classroom, weekly timetable periods, guardian consent links.
-- Teacher ids everywhere below are users.id of the TEACHER account (the same id the JWT subject and /topic/teacher/{id}/alerts use).

CREATE TABLE teacher_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    phone VARCHAR(32),
    qualification VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_teacher_profiles_school ON teacher_profiles (school_id);

-- Teachers invited before profiles existed get one derived from their login email.
INSERT INTO teacher_profiles (user_id, school_id, first_name, last_name, phone)
SELECT u.id, u.school_id, split_part(u.email, '@', 1), NULL, u.phone
  FROM users u WHERE u.role = 'TEACHER' AND u.school_id IS NOT NULL;

ALTER TABLE classrooms ADD COLUMN class_teacher_id BIGINT REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE classrooms ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
-- A teacher is class teacher of AT MOST ONE classroom.
CREATE UNIQUE INDEX uq_classrooms_class_teacher ON classrooms (class_teacher_id) WHERE class_teacher_id IS NOT NULL;

CREATE TABLE timetable_periods (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL REFERENCES schools (id) ON DELETE CASCADE,
    classroom_id BIGINT NOT NULL REFERENCES classrooms (id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    subject VARCHAR(80) NOT NULL,
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_timetable_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_timetable_times CHECK (end_time > start_time)
);
CREATE INDEX idx_timetable_teacher ON timetable_periods (teacher_id, day_of_week, start_time);
CREATE INDEX idx_timetable_classroom ON timetable_periods (classroom_id);

-- Guardian consent: single-use, expiring link sent by SMS. Only a hash of the token is stored.
CREATE TABLE consent_tokens (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    school_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    response BOOLEAN,
    created_by_user_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_consent_used CHECK ((used_at IS NULL AND response IS NULL) OR (used_at IS NOT NULL AND response IS NOT NULL))
);
CREATE INDEX idx_consent_tokens_student ON consent_tokens (student_id, created_at DESC);

ALTER TABLE students ADD COLUMN consent_at TIMESTAMPTZ;
ALTER TABLE students ADD COLUMN consent_source VARCHAR(32);
-- Rows consented before provenance existed: be honest about it.
UPDATE students SET consent_source = 'LEGACY' WHERE marketing_consent = TRUE;

-- Backs the teacher alerts query (school drops the school marked ABSENT) and the classroom summary joins.
CREATE INDEX idx_arrival_alert_drop ON arrival_verifications (arrival_date, student_id)
    WHERE kind = 'SCHOOL_DROP' AND school_confirm = 'ABSENT';
