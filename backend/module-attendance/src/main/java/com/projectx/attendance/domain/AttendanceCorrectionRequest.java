package com.projectx.attendance.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/**
 * C3: once a day's attendance has been submitted, a TEACHER can no longer edit the stored row
 * directly (see AttendanceService.bulk's teacher-lock check) - they file one of these instead.
 * {@code previousStatus} is a snapshot of what {@code school_attendance} held for this student+date
 * at request time (null if that row did not exist yet) - purely informational/audit, not re-checked
 * at decision time; a SCHOOL_ADMIN decision always re-reads and overwrites whatever is actually
 * stored then, same upsert path as a direct admin edit.
 */
@Entity
@Table(name = "attendance_correction_requests")
public class AttendanceCorrectionRequest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 16)
    private AttendanceStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_status", nullable = false, length = 16)
    private AttendanceStatus requestedStatus;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CorrectionStatus status = CorrectionStatus.PENDING;

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;

    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected AttendanceCorrectionRequest() {
    }

    public AttendanceCorrectionRequest(Long schoolId, Long classroomId, LocalDate date, Long studentId,
                                       AttendanceStatus previousStatus, AttendanceStatus requestedStatus, String reason,
                                       Long requestedByUserId) {
        this.schoolId = schoolId;
        this.classroomId = classroomId;
        this.date = date;
        this.studentId = studentId;
        this.previousStatus = previousStatus;
        this.requestedStatus = requestedStatus;
        this.reason = reason;
        this.requestedByUserId = requestedByUserId;
    }

    public void decide(CorrectionStatus status, Long decidedByUserId, String note) {
        this.status = status;
        this.decidedByUserId = decidedByUserId;
        this.decisionNote = note;
        this.decidedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getStudentId() {
        return studentId;
    }

    public AttendanceStatus getPreviousStatus() {
        return previousStatus;
    }

    public AttendanceStatus getRequestedStatus() {
        return requestedStatus;
    }

    public String getReason() {
        return reason;
    }

    public CorrectionStatus getStatus() {
        return status;
    }

    public Long getRequestedByUserId() {
        return requestedByUserId;
    }

    public Long getDecidedByUserId() {
        return decidedByUserId;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
