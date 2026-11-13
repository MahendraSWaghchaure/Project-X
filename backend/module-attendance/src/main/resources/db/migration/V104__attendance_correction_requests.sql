-- module-attendance owns Flyway version range V100+ (continues V103).
-- C3: attendance correction-request workflow (a TEACHER can no longer directly edit a submitted
-- day's attendance row - see AttendanceService.bulk's teacher-lock check added alongside this).
CREATE TABLE attendance_correction_requests (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    school_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    student_id BIGINT NOT NULL,
    previous_status VARCHAR(16),
    requested_status VARCHAR(16) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    requested_by_user_id BIGINT NOT NULL,
    decided_by_user_id BIGINT,
    decision_note VARCHAR(500),
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attendance_correction_school_status ON attendance_correction_requests (school_id, status);
CREATE INDEX idx_attendance_correction_classroom_date ON attendance_correction_requests (classroom_id, attendance_date);
CREATE INDEX idx_attendance_correction_requested_by ON attendance_correction_requests (requested_by_user_id);
