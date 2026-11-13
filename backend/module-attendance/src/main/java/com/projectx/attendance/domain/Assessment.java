package com.projectx.attendance.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "assessments")
public class Assessment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "exam_name", nullable = false, length = 120)
    private String examName;

    @Column(nullable = false, length = 80)
    private String subject;

    @Column(name = "max_marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "marks_obtained", nullable = false, precision = 7, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "recorded_by_user_id", nullable = false)
    private Long recordedByUserId;

    protected Assessment() {
    }

    public Assessment(Long schoolId, Long studentId, String examName, String subject, BigDecimal maxMarks,
                      BigDecimal marksObtained, LocalDate examDate, Long recordedByUserId) {
        this.schoolId = schoolId;
        this.studentId = studentId;
        this.examName = examName;
        this.subject = subject;
        this.maxMarks = maxMarks;
        this.marksObtained = marksObtained;
        this.examDate = examDate;
        this.recordedByUserId = recordedByUserId;
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

    public String getExamName() {
        return examName;
    }

    public String getSubject() {
        return subject;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public BigDecimal getMarksObtained() {
        return marksObtained;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public Long getRecordedByUserId() {
        return recordedByUserId;
    }
}
