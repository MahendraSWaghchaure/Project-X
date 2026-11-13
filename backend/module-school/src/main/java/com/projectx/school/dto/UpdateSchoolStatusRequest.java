package com.projectx.school.dto;

import com.projectx.school.domain.SchoolStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Suspending is the "delete" a platform admin performs on a school - reversible, and doesn't touch its data
 * (vehicles/routes/students/trips all stay intact). {@code reason} is mandatory when suspending (shown on the
 * schools list) and ignored when re-activating.
 */
public record UpdateSchoolStatusRequest(@NotNull SchoolStatus status, @Size(max = 500) String reason) {

    /** Back-compat for callers that only ever re-activate. */
    public UpdateSchoolStatusRequest(SchoolStatus status) {
        this(status, null);
    }
}
