package com.projectx.grievance.dto;

import com.projectx.common.security.Role;
import com.projectx.grievance.domain.Grievance;
import com.projectx.grievance.domain.GrievanceCategory;
import com.projectx.grievance.domain.GrievanceStatus;
import java.time.Instant;

public record GrievanceResponse(
        Long id, Long schoolId, Long raisedByUserId, Role raisedByRole, String raisedByEmail,
        GrievanceCategory category, String subject, String description, GrievanceStatus status,
        String resolutionNote, Long resolvedByUserId, Instant resolvedAt, Instant createdAt, Instant updatedAt) {

    public static GrievanceResponse from(Grievance g) {
        return new GrievanceResponse(g.getId(), g.getSchoolId(), g.getRaisedByUserId(), g.getRaisedByRole(),
                g.getRaisedByEmail(), g.getCategory(), g.getSubject(), g.getDescription(), g.getStatus(),
                g.getResolutionNote(), g.getResolvedByUserId(), g.getResolvedAt(), g.getCreatedAt(), g.getUpdatedAt());
    }
}
