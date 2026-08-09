package com.projectx.grievance.dto;

import com.projectx.grievance.domain.GrievanceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * schoolId is supplied by the caller rather than read off the JWT because PARENT tokens carry no
 * schoolId claim (a parent isn't scoped to one school the way a SCHOOL_ADMIN/DRIVER is) - see
 * CurrentUser's javadoc in module-common.
 */
public record CreateGrievanceRequest(
        Long schoolId,
        @NotNull GrievanceCategory category,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 4000) String description) {
}
