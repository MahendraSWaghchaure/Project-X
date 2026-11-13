package com.projectx.grievance.dto;

import com.projectx.grievance.domain.GrievanceCategory;
import com.projectx.grievance.domain.GrievanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * schoolId is supplied by the caller rather than read off the JWT because PARENT tokens carry no
 * schoolId claim (a parent isn't scoped to one school the way a SCHOOL_ADMIN/DRIVER is) - see
 * CurrentUser's javadoc in module-common.
 *
 * <p>{@code type} is the structured complaint type (validated against the caller's role); {@code otherDescription} is
 * required when it is OTHER. The legacy {@code category} is still accepted: when only it is sent the type is derived
 * from it, and when only {@code type} is sent the category is derived. At least one of the two is required, and
 * {@code subject} defaults to the type name when omitted. vehicleId/driverId/childId/tripId optionally link the
 * complaint to what it is about; they are verified against the caller's school / guardianship server-side.
 */
public record CreateGrievanceRequest(
        Long schoolId,
        GrievanceCategory category,
        GrievanceType type,
        @Size(max = 500) String otherDescription,
        Long vehicleId,
        Long driverId,
        Long childId,
        Long tripId,
        @Size(max = 200) String subject,
        @NotBlank @Size(max = 4000) String description) {

    /** The original four-field (legacy category) shape. */
    public CreateGrievanceRequest(Long schoolId, GrievanceCategory category, String subject, String description) {
        this(schoolId, category, null, null, null, null, null, null, subject, description);
    }
}
