package com.projectx.attendance.dto;

import com.projectx.attendance.domain.Assessment;
import com.projectx.attendance.domain.StudentComment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class AcademicDtos {

    private AcademicDtos() {
    }

    public record AssessmentRequest(
            @NotBlank @Size(max = 120) String examName,
            @NotBlank @Size(max = 80) String subject,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 5, fraction = 2) BigDecimal maxMarks,
            @NotNull @DecimalMin(value = "0") @Digits(integer = 5, fraction = 2) BigDecimal marksObtained,
            @NotNull LocalDate examDate) {
    }

    public record AssessmentResponse(Long id, Long studentId, String examName, String subject, BigDecimal maxMarks,
                                     BigDecimal marksObtained, LocalDate examDate, Long recordedByUserId, Instant createdAt) {
        public static AssessmentResponse from(Assessment a) {
            return new AssessmentResponse(a.getId(), a.getStudentId(), a.getExamName(), a.getSubject(), a.getMaxMarks(),
                    a.getMarksObtained(), a.getExamDate(), a.getRecordedByUserId(), a.getCreatedAt());
        }
    }

    public record CommentRequest(@NotBlank @Size(max = 2000) String text) {
    }

    public record CommentResponse(Long id, Long studentId, String text, Long authorUserId, String authorEmail, Instant createdAt) {
        public static CommentResponse from(StudentComment c) {
            return new CommentResponse(c.getId(), c.getStudentId(), c.getText(), c.getAuthorUserId(), c.getAuthorEmail(),
                    c.getCreatedAt());
        }
    }
}
