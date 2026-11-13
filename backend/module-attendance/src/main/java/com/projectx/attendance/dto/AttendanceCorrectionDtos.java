package com.projectx.attendance.dto;

import com.projectx.attendance.domain.AttendanceCorrectionRequest;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.domain.CorrectionDecision;
import com.projectx.attendance.domain.CorrectionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class AttendanceCorrectionDtos {

    private AttendanceCorrectionDtos() {
    }

    public record CreateCorrectionRequest(
            @NotNull Long classroomId, @NotNull LocalDate date, @NotNull Long studentId,
            @NotNull AttendanceStatus requestedStatus, @NotBlank @Size(max = 500) String reason) {
    }

    /** {@code note} is required when rejecting (enforced in the service, not here - APPROVE needs none). */
    public record DecisionRequest(@NotNull CorrectionDecision decision, @Size(max = 500) String note) {
    }

    public record CorrectionResponse(
            Long id, Long schoolId, Long classroomId, String classroomName, LocalDate date, Long studentId,
            String studentName, AttendanceStatus previousStatus, AttendanceStatus requestedStatus, String reason,
            CorrectionStatus status, Long requestedByUserId, Long decidedByUserId, String decisionNote,
            Instant decidedAt, Instant createdAt) {

        public static CorrectionResponse from(AttendanceCorrectionRequest r) {
            return from(r, null, null);
        }

        public static CorrectionResponse from(AttendanceCorrectionRequest r, String classroomName, String studentName) {
            return new CorrectionResponse(r.getId(), r.getSchoolId(), r.getClassroomId(), classroomName, r.getDate(),
                    r.getStudentId(), studentName, r.getPreviousStatus(), r.getRequestedStatus(), r.getReason(),
                    r.getStatus(), r.getRequestedByUserId(), r.getDecidedByUserId(), r.getDecisionNote(),
                    r.getDecidedAt(), r.getCreatedAt());
        }
    }
}
