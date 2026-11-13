package com.projectx.grievance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One entry of a grievance's timeline: it was opened, then each status change with the note the resolver left. */
@Entity
@Table(name = "grievance_events")
public class GrievanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grievance_id", nullable = false)
    private Long grievanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GrievanceStatus status;

    @Column(length = 2000)
    private String note;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected GrievanceEvent() {
    }

    public GrievanceEvent(Long grievanceId, GrievanceStatus status, String note, Long actorUserId) {
        this.grievanceId = grievanceId;
        this.status = status;
        this.note = note;
        this.actorUserId = actorUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getGrievanceId() {
        return grievanceId;
    }

    public GrievanceStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
