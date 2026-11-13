package com.projectx.attendance.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/**
 * "Safe arrival" record opened by a driver event and closed by school attendance and/or the
 * parent. Three independent actors (driver, school staff, guardians) can touch the same row
 * concurrently, so it is {@code @Version}ed: a lost update here could silently hide an ALERT.
 * {@code outcome} is derived (see {@link ArrivalOutcome#compute}) and re-derived on every change,
 * stored only so it can be indexed/filtered/counted.
 */
@Entity
@Table(name = "arrival_verifications")
public class Arrival extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "child_name", nullable = false, length = 220)
    private String childName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ArrivalKind kind;

    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "driver_event_at", nullable = false)
    private Instant driverEventAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "school_confirm", nullable = false, length = 16)
    private SchoolConfirm schoolConfirm = SchoolConfirm.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "parent_response", nullable = false, length = 16)
    private ParentResponse parentResponse = ParentResponse.PENDING;

    @Column(name = "parent_note", length = 500)
    private String parentNote;

    @Column(name = "parent_responded_at")
    private Instant parentRespondedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ArrivalOutcome outcome = ArrivalOutcome.PENDING;

    protected Arrival() {
    }

    public Arrival(Long schoolId, Long tripId, Long childId, Long studentId, String childName, ArrivalKind kind,
                   LocalDate arrivalDate, Instant driverEventAt) {
        this.schoolId = schoolId;
        this.tripId = tripId;
        this.childId = childId;
        this.studentId = studentId;
        this.childName = childName;
        this.kind = kind;
        this.arrivalDate = arrivalDate;
        this.driverEventAt = driverEventAt;
    }

    /** A later driver event for the same arrival moves the "driver said" timestamp forward (never backwards). */
    public boolean advanceDriverEventAt(Instant at) {
        if (at == null || !at.isAfter(driverEventAt)) {
            return false;
        }
        this.driverEventAt = at;
        return true;
    }

    /** The child was linked to a roster student after the arrival was opened. */
    public void attachStudent(Long studentId) {
        this.studentId = studentId;
    }

    /** @return true if the outcome changed. */
    public boolean applySchoolConfirm(SchoolConfirm confirm) {
        this.schoolConfirm = confirm;
        return recomputeOutcome();
    }

    /** @return true if the outcome changed. */
    public boolean applyParentResponse(ParentResponse response, String note) {
        this.parentResponse = response;
        this.parentNote = note;
        this.parentRespondedAt = Instant.now();
        return recomputeOutcome();
    }

    public boolean recomputeOutcome() {
        ArrivalOutcome next = ArrivalOutcome.compute(kind, schoolConfirm, parentResponse);
        boolean changed = next != outcome;
        this.outcome = next;
        return changed;
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

    public Long getTripId() {
        return tripId;
    }

    public Long getChildId() {
        return childId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getChildName() {
        return childName;
    }

    public ArrivalKind getKind() {
        return kind;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public Instant getDriverEventAt() {
        return driverEventAt;
    }

    public SchoolConfirm getSchoolConfirm() {
        return schoolConfirm;
    }

    public ParentResponse getParentResponse() {
        return parentResponse;
    }

    public String getParentNote() {
        return parentNote;
    }

    public ArrivalOutcome getOutcome() {
        return outcome;
    }
}
