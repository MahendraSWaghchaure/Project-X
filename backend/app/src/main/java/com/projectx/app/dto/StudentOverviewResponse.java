package com.projectx.app.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the student detail page needs beyond the roster row, in one call. {@code transport}, {@code insurance} and
 * {@code attendance} are null for a teacher who only lectures the student's class (they get the student, redacted, and nothing
 * else); {@code transport} and {@code insurance} are also null for a non-subscribed student. Insurance is read-only here: it is the
 * policy linked to the student's transport child.
 */
public record StudentOverviewResponse(StudentResponse student, Transport transport, Insurance insurance, List<AttendanceDay> attendance) {

    public record Transport(Long childId, String routeName, String stopLabel, String vehicleNo) {
    }

    public record Insurance(String policyNumber, String provider, String status, @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate startDate,
                            @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate endDate) {
    }

    public record AttendanceDay(@JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate date, AttendanceStatus status) {
    }
}
