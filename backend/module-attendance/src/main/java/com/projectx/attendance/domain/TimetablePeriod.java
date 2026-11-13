package com.projectx.attendance.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

/** One weekly teaching slot: {@code teacherId} (users.id) teaches {@code subject} to {@code classroomId} on {@code dayOfWeek} (1 = Monday ... 7 = Sunday). */
@Entity
@Table(name = "timetable_periods")
public class TimetablePeriod extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(nullable = false, length = 80)
    private String subject;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected TimetablePeriod() {
    }

    public TimetablePeriod(Long schoolId, Long classroomId, Long teacherId, String subject, int dayOfWeek,
                           LocalTime startTime, LocalTime endTime) {
        this.schoolId = schoolId;
        this.classroomId = classroomId;
        this.teacherId = teacherId;
        this.subject = subject;
        this.dayOfWeek = (short) dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Long getClassroomId() {
        return classroomId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public String getSubject() {
        return subject;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
