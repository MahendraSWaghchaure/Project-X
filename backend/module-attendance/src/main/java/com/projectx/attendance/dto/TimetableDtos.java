package com.projectx.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.projectx.attendance.domain.TimetablePeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Timetable periods, the teacher's own portal views (periods / classrooms) and the C4 "student dropped but marked absent" alert. */
public final class TimetableDtos {

    private TimetableDtos() {
    }

    /** {@code dayOfWeek}: 1 = Monday ... 7 = Sunday. Times are school-local wall-clock ("08:30"). */
    public record PeriodRequest(
            @NotNull Long teacherId,
            @NotBlank @Size(max = 80) String subject,
            @NotNull @Min(1) @Max(7) Integer dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime) {
    }

    public record PeriodResponse(Long id, Long classroomId, String classroomName, Long teacherId, String teacherName,
                                 String subject, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        public static PeriodResponse from(TimetablePeriod p, String classroomName, String teacherName) {
            return new PeriodResponse(p.getId(), p.getClassroomId(), classroomName, p.getTeacherId(), teacherName,
                    p.getSubject(), p.getDayOfWeek(), p.getStartTime(), p.getEndTime());
        }
    }

    /** GET /teachers/me/periods: only the days that have periods, Monday first, periods by start time. */
    public record DaySchedule(int dayOfWeek, List<PeriodResponse> periods) {
    }

    /** role: CLASS_TEACHER (also lists the subjects they teach there, if any) or LECTURER. */
    public record TeacherClassroom(ClassroomDtos.ClassroomResponse classroom, String role, List<String> subjects) {
    }

    /**
     * A child the driver dropped at school whom the school register marks ABSENT. {@code guardianName}/{@code guardianPhone}
     * are shown only to the class teacher of the class (the only teacher who ever receives alerts).
     */
    public record TeacherAlert(Long arrivalId, Long studentId, String studentName, Long classroomId, String classroomName,
                               @JsonFormat(shape = JsonFormat.Shape.STRING) Instant droppedAt,
                               @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate date,
                               String driverName, String vehicleNo, String guardianName, String guardianPhone) {
    }
}
