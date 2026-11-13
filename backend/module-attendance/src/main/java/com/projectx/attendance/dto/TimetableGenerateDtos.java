package com.projectx.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

/** C5: the automatic timetable generator's request/response shapes. See TimetableGeneratorService for the algorithm itself. */
public final class TimetableGenerateDtos {

    private TimetableGenerateDtos() {
    }

    /** One recess/lunch break: no period slot may overlap [start, start + durationMinutes). */
    public record BreakInput(@NotNull LocalTime start, @NotNull @Min(5) @Max(180) Integer durationMinutes) {
    }

    /** A teacher the admin wants the generator to use, and every subject they are willing to teach. */
    public record TeacherInput(@NotNull Long teacherId, @NotEmpty List<@NotBlank String> subjects) {
    }

    /** How many periods/week {@code subject} needs for every section of class {@code className}. */
    public record SubjectLoadInput(@NotBlank String className, @NotBlank String subject, @NotNull @Min(1) @Max(20) Integer periodsPerWeek) {
    }

    /**
     * {@code workingDays} null/empty defaults to Mon-Sat ({@code [1,2,3,4,5,6]}, day-of-week 1=Monday). {@code classNames}
     * selects whole classes (every section sharing that {@code Classroom.name}, e.g. "Grade 3" pulls in 3-A, 3-B, ...) -
     * not individual classroom/section ids, per the plan's "classes to include (not sections)".
     */
    public record GenerateTimetableRequest(
            @NotNull @Min(5) @Max(180) Integer periodLengthMinutes,
            @NotNull LocalTime firstPeriodStart,
            @NotNull LocalTime lastPeriodEnd,
            List<@Valid BreakInput> breaks,
            List<@Min(1) @Max(7) Integer> workingDays,
            @NotEmpty List<@Valid TeacherInput> teachers,
            @NotEmpty List<@NotBlank String> classNames,
            @NotEmpty List<@Valid SubjectLoadInput> subjectLoads) {
    }

    public record GeneratedPeriod(Long classroomId, String classroomName, int dayOfWeek, LocalTime startTime, LocalTime endTime,
                                  Long teacherId, String teacherName, String subject, boolean classTeacherPeriod) {
    }

    /**
     * {@code dayOfWeek}/{@code startTime}/{@code endTime} are null for a classroom-level "ran out of week" shortfall (not
     * tied to one slot - see TimetableGeneratorService's per-classroom leftover-demand report); otherwise it is one specific
     * slot the greedy pass could not fill.
     */
    public record UnfilledSlot(Long classroomId, String classroomName, Integer dayOfWeek, LocalTime startTime, LocalTime endTime,
                               String subject, String reason) {
    }

    /** {@code written} is false for {@code /preview} (dry run, nothing persisted) and true for {@code /generate}. */
    public record TimetableGenerationResponse(List<GeneratedPeriod> periods, List<UnfilledSlot> unfilled, boolean written) {
    }
}
