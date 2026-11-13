package com.projectx.attendance.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class StaffDtos {

    private StaffDtos() {
    }

    /**
     * {@code name} (first and last name in one string) falls back to the email's local part when omitted;
     * {@code classTeacherOfClassroomId} optionally makes the new teacher the class teacher of that classroom.
     */
    public record InviteTeacherRequest(
            @NotBlank @Email String email,
            @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
            @Size(max = 200) String name,
            @Size(max = 200) String qualification,
            Long classTeacherOfClassroomId) {

        public InviteTeacherRequest(String email, String phone) {
            this(email, phone, null, null, null);
        }
    }

    /**
     * Edit a teacher. A full replace of the editable fields: {@code classTeacherOfClassroomId} null means "not a class
     * teacher" (removes an existing assignment).
     */
    public record UpdateTeacherRequest(
            @NotBlank @Size(max = 200) String name,
            @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
            @Size(max = 200) String qualification,
            Long classTeacherOfClassroomId) {
    }

    /** inviteToken is returned for the same reason as InviteParentResponse: invite delivery by email/SMS is not wired with real credentials yet. */
    public record InviteTeacherResponse(Long userId, String email, String inviteToken) {
    }

    /**
     * {@code status} is the raw account status (INVITED / ACTIVE / DISABLED); {@code inviteStatus} is what the table shows:
     * INVITED (link still valid), EXPIRED (never accepted and the link ran out - use resend), ACTIVE, DISABLED.
     * {@code periodsPerWeek} counts the teacher's timetable periods.
     */
    public record TeacherResponse(Long userId, String email, String phone, String status, String name, String firstName,
                                  String lastName, String qualification, String inviteStatus, Instant inviteExpiresAt,
                                  Long classTeacherOfClassroomId, String classTeacherOfClassroomName, long periodsPerWeek) {
    }

    /** Guardian contact of a consented, non-subscribed student. {@code name}/{@code phone}/{@code email} are the guardian's; {@code school} is the school's name. */
    public record LeadResponse(Long studentId, String name, String phone, String email, String school, Long schoolId,
                               String leadSource) {
    }
}
