package com.projectx.attendance.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;

/**
 * One row per student of a school, subscribed to Sunju transport or not. {@code childId} is a
 * cross-module id reference to module-people's Child (set = subscribed). {@code version} guards two
 * admins editing the same roster entry at once (surfaced as 409 by GlobalExceptionHandler).
 */
@Entity
@Table(name = "students")
public class Student extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "students_seq")
    @SequenceGenerator(name = "students_seq", sequenceName = "students_seq", allocationSize = 50)
    private Long id;

    @Version
    private Long version;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "classroom_id")
    private Long classroomId;

    @Column(name = "roll_no", length = 32)
    private String rollNo;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 16)
    private String gender;

    @Column(name = "guardian_name", nullable = false, length = 200)
    private String guardianName;

    @Column(name = "guardian_phone", nullable = false, length = 32)
    private String guardianPhone;

    @Column(name = "guardian_email")
    private String guardianEmail;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent = false;

    @Column(name = "lead_source", length = 64)
    private String leadSource;

    @Column(name = "child_id", unique = true)
    private Long childId;

    /** When marketingConsent last changed, and how: SMS_LINK (guardian answered the link), ADMIN_EDIT, IMPORT, LEGACY. */
    @Column(name = "consent_at")
    private java.time.Instant consentAt;

    @Column(name = "consent_source", length = 32)
    private String consentSource;

    protected Student() {
    }

    public Student(Long schoolId) {
        this.schoolId = schoolId;
    }

    public void applyDetails(Long classroomId, String rollNo, String firstName, String lastName, LocalDate dateOfBirth,
                             String gender, String guardianName, String guardianPhone, String guardianEmail,
                             boolean marketingConsent, String leadSource) {
        this.classroomId = classroomId;
        this.rollNo = rollNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
        this.guardianEmail = guardianEmail;
        this.marketingConsent = marketingConsent;
        this.leadSource = leadSource;
    }

    public java.time.Instant getConsentAt() {
        return consentAt;
    }

    public String getConsentSource() {
        return consentSource;
    }

    /** Records that the consent flag was (re)set by {@code source} just now. */
    public void stampConsent(String source) {
        this.consentAt = java.time.Instant.now();
        this.consentSource = source;
    }

    public boolean isSubscribed() {
        return childId != null;
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

    public String getRollNo() {
        return rollNo;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public String getGuardianEmail() {
        return guardianEmail;
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    public String getLeadSource() {
        return leadSource;
    }

    public Long getChildId() {
        return childId;
    }

    public void linkChild(Long childId) {
        this.childId = childId;
    }

    public void unlinkChild() {
        this.childId = null;
    }
}
