package com.projectx.grievance.dto;

import com.projectx.common.security.Role;
import com.projectx.grievance.domain.Grievance;
import com.projectx.grievance.domain.GrievanceCategory;
import com.projectx.grievance.domain.GrievanceEvent;
import com.projectx.grievance.domain.GrievanceStatus;
import com.projectx.grievance.domain.GrievanceType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The table row / detail of a grievance. The display names ({@code complainantName}, {@code driverName}, {@code vehicleNo},
 * {@code childName}, {@code schoolName}) are resolved by the GrievanceDirectory in a batch and may be null (no link, or the
 * record is gone). {@code daysOpen} counts whole days from creation to resolution (or to now while still open).
 * {@code timeline} is only filled by the detail endpoint.
 */
public record GrievanceResponse(
        Long id, Long schoolId, Long raisedByUserId, Role raisedByRole, String raisedByEmail,
        GrievanceCategory category, String subject, String description, GrievanceStatus status,
        String resolutionNote, Long resolvedByUserId, Instant resolvedAt, Instant createdAt, Instant updatedAt,
        GrievanceType type, String otherDescription, Long vehicleId, Long driverId, Long childId, Long tripId,
        String complainantName, String driverName, String vehicleNo, String childName, String schoolName,
        long daysOpen, List<TimelineEntry> timeline) {

    public record TimelineEntry(GrievanceStatus status, String note, Long actorUserId, Instant at) {
        public static TimelineEntry from(GrievanceEvent e) {
            return new TimelineEntry(e.getStatus(), e.getNote(), e.getActorUserId(), e.getCreatedAt());
        }
    }

    public static GrievanceResponse from(Grievance g) {
        return from(g, null, null, null, null, null, null);
    }

    public static GrievanceResponse from(Grievance g, String complainantName, String driverName, String vehicleNo,
                                         String childName, String schoolName, List<TimelineEntry> timeline) {
        Instant end = g.getResolvedAt() != null ? g.getResolvedAt() : Instant.now();
        long days = g.getCreatedAt() == null ? 0 : Math.max(0, Duration.between(g.getCreatedAt(), end).toDays());
        return new GrievanceResponse(g.getId(), g.getSchoolId(), g.getRaisedByUserId(), g.getRaisedByRole(),
                g.getRaisedByEmail(), g.getCategory(), g.getSubject(), g.getDescription(), g.getStatus(),
                g.getResolutionNote(), g.getResolvedByUserId(), g.getResolvedAt(), g.getCreatedAt(), g.getUpdatedAt(),
                g.getType(), g.getOtherDescription(), g.getVehicleId(), g.getDriverId(), g.getChildId(), g.getTripId(),
                complainantName, driverName, vehicleNo, childName, schoolName, days, timeline);
    }
}
