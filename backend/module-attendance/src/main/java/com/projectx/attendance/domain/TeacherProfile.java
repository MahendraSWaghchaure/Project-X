package com.projectx.attendance.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Display profile of a TEACHER login (name, phone, qualification). The login itself (email, password, invite status) is the users row. */
@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 32)
    private String phone;

    @Column(length = 200)
    private String qualification;

    protected TeacherProfile() {
    }

    public TeacherProfile(Long userId, Long schoolId, String firstName, String lastName, String phone, String qualification) {
        this.userId = userId;
        this.schoolId = schoolId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.qualification = qualification;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getQualification() {
        return qualification;
    }

    public String fullName() {
        return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
    }

    public void update(String firstName, String lastName, String phone, String qualification) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.qualification = qualification;
    }
}
