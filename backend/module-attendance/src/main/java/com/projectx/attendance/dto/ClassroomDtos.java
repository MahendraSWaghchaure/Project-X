package com.projectx.attendance.dto;

import com.projectx.attendance.domain.Classroom;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ClassroomDtos {

    private ClassroomDtos() {
    }

    /** {@code classTeacherId} (a TEACHER's userId) is optional: assign the class teacher while creating the class. */
    public record ClassroomRequest(@NotBlank @Size(max = 64) String name, @Size(max = 32) String section, Long classTeacherId) {

        public ClassroomRequest(String name, String section) {
            this(name, section, null);
        }
    }

    /** Edit name / section. {@code version} (from the last read) turns a concurrent edit into a 409 instead of a silent overwrite. */
    public record UpdateClassroomRequest(@NotBlank @Size(max = 64) String name, @Size(max = 32) String section, Long version) {
    }

    /** {@code teacherId} null = remove the class teacher. */
    public record AssignClassTeacherRequest(Long teacherId) {
    }

    /** {@code classTeacherName}/{@code studentCount} come from batched lookups (names from teacher profiles, count from one GROUP BY). */
    public record ClassroomResponse(Long id, Long schoolId, String name, String section, Long classTeacherId,
                                    String classTeacherName, long studentCount, Long version) {
        public static ClassroomResponse from(Classroom c) {
            return from(c, null, 0);
        }

        public static ClassroomResponse from(Classroom c, String classTeacherName, long studentCount) {
            return new ClassroomResponse(c.getId(), c.getSchoolId(), c.getName(), c.getSection(), c.getClassTeacherId(),
                    classTeacherName, studentCount, c.getVersion());
        }
    }

    /**
     * One row of the classrooms hub. {@code present} counts PRESENT + LATE (a late child is in school); {@code late} is the
     * LATE subset, {@code unmarked} = totalStudents - present - absent.
     */
    public record ClassroomSummary(Long id, String name, String section, Long classTeacherId, String classTeacherName,
                                   long totalStudents, long present, long absent, long late, long unmarked) {
    }

    public record SummaryQuery(@NotNull java.time.LocalDate date) {
    }
}
