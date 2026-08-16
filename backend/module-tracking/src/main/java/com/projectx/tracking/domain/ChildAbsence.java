package com.projectx.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** A parent's advance notice that their child won't ride on a given date, so the driver isn't shown a phantom pickup. */
@Entity
@Table(name = "child_absences")
public class ChildAbsence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "absence_date", nullable = false)
    private LocalDate absenceDate;

    @Column(name = "marked_by_user_id", nullable = false)
    private Long markedByUserId;

    protected ChildAbsence() {
    }

    public ChildAbsence(Long childId, LocalDate absenceDate, Long markedByUserId) {
        this.childId = childId;
        this.absenceDate = absenceDate;
        this.markedByUserId = markedByUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getChildId() {
        return childId;
    }

    public LocalDate getAbsenceDate() {
        return absenceDate;
    }

    public Long getMarkedByUserId() {
        return markedByUserId;
    }
}
