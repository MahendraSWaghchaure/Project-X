package com.projectx.attendance.dto;

import com.projectx.attendance.domain.Student;
import com.projectx.attendance.domain.Subscription;
import com.projectx.attendance.dto.ConsentDtos.ConsentStatus;
import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class StudentDtos {

    private StudentDtos() {
    }

    /**
     * Create/update/import row. {@code version} is optional and only meaningful on PUT: when sent
     * and stale the update fails with 409 (optimistic locking) instead of silently overwriting a
     * colleague's edit.
     */
    public record StudentRequest(
            Long classroomId,
            @Size(max = 32) String rollNo,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            LocalDate dob,
            @Size(max = 16) String gender,
            @NotBlank @Size(max = 200) String guardianName,
            @NotBlank @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String guardianPhone,
            @Email @Size(max = 255) String guardianEmail,
            Boolean marketingConsent,
            @Size(max = 64) String leadSource,
            Long version) {
    }

    /**
     * {@code consentStatus} (NOT_REQUESTED | SENT | EXPIRED | GRANTED | DECLINED) and {@code consentExpiresAt} (while SENT)
     * describe the guardian-consent link; {@code consentAt}/{@code consentSource} (SMS_LINK | ADMIN_EDIT | IMPORT | LEGACY)
     * record when and how {@code marketingConsent} last changed.
     *
     * <p>A teacher who is only a LECTURER of the student's class gets the guardian fields and every consent field nulled
     * ({@link #withoutGuardianContact()}) - they may record marks, not see who to call.
     */
    public record StudentResponse(
            Long id, Long schoolId, Long classroomId, String rollNo, String firstName, String lastName, LocalDate dob,
            String gender, String guardianName, String guardianPhone, String guardianEmail, boolean marketingConsent,
            String leadSource, Subscription subscription, Long childId, Long version,
            ConsentStatus consentStatus, Instant consentExpiresAt, Instant consentAt, String consentSource) {

        public static StudentResponse from(Student s) {
            return from(s, s.isMarketingConsent() ? ConsentStatus.GRANTED : ConsentStatus.NOT_REQUESTED, null);
        }

        public static StudentResponse from(Student s, ConsentStatus consentStatus, Instant consentExpiresAt) {
            return new StudentResponse(s.getId(), s.getSchoolId(), s.getClassroomId(), s.getRollNo(), s.getFirstName(),
                    s.getLastName(), s.getDateOfBirth(), s.getGender(), s.getGuardianName(), s.getGuardianPhone(),
                    s.getGuardianEmail(), s.isMarketingConsent(), s.getLeadSource(),
                    s.isSubscribed() ? Subscription.SUBSCRIBED : Subscription.NON_SUBSCRIBED, s.getChildId(), s.getVersion(),
                    consentStatus, consentExpiresAt, s.getConsentAt(), s.getConsentSource());
        }

        public StudentResponse withoutGuardianContact() {
            return new StudentResponse(id, schoolId, classroomId, rollNo, firstName, lastName, dob, gender, null, null, null,
                    false, null, subscription, childId, version, null, null, null, null);
        }
    }

    public record LinkChildRequest(@NotNull Long childId) {
    }

    public record ImportResult(int created, int updated) {
    }

    /**
     * C2: one row of the school-admin "Students & Guardians" directory - {@code className}/{@code section} are the
     * classroom's own display fields (null if the student has no classroom yet), not just its id, so the table needs
     * no second lookup per row.
     */
    public record DirectoryRow(Long studentId, String studentName, Long classroomId, String className, String section,
                               String guardianName, String guardianPhone, String guardianEmail, Subscription subscription) {
    }

    /** E4: minimal shape for a parent's "pick the other student" grievance dialog - name + class + section ONLY, no guardian/PII. */
    public record StudentSearchResult(Long studentId, String name, String className, String section) {
    }
}
