package com.projectx.attendance.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_comments")
public class StudentComment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(name = "author_email")
    private String authorEmail;

    protected StudentComment() {
    }

    public StudentComment(Long schoolId, Long studentId, String text, Long authorUserId, String authorEmail) {
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.text = text;
        this.authorUserId = authorUserId;
        this.authorEmail = authorEmail;
    }

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getText() {
        return text;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }
}
