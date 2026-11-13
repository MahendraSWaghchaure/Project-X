package com.projectx.attendance.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * {@code classTeacherId} is the users.id of the TEACHER who is class teacher of this classroom (cross-module id, DB-unique:
 * a teacher is class teacher of at most one classroom). {@code version} guards two admins editing the same class at once.
 */
@Entity
@Table(name = "classrooms")
public class Classroom extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32)
    private String section;

    @Column(name = "class_teacher_id")
    private Long classTeacherId;

    protected Classroom() {
    }

    public Classroom(Long schoolId, String name, String section) {
        this.schoolId = schoolId;
        this.name = name;
        this.section = section;
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

    public String getName() {
        return name;
    }

    public String getSection() {
        return section;
    }

    public Long getClassTeacherId() {
        return classTeacherId;
    }

    public void setClassTeacherId(Long classTeacherId) {
        this.classTeacherId = classTeacherId;
    }

    public void rename(String name, String section) {
        this.name = name;
        this.section = section;
    }

    /** "Grade 3-A" style label used in alerts and period lists. */
    public String displayName() {
        return section == null || section.isBlank() ? name : name + "-" + section;
    }
}
