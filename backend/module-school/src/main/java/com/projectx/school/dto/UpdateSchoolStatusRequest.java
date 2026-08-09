package com.projectx.school.dto;

import com.projectx.school.domain.SchoolStatus;
import jakarta.validation.constraints.NotNull;

/** Suspending is the "delete" a platform admin performs on a school - reversible, and doesn't touch its data (vehicles/routes/students/trips all stay intact). */
public record UpdateSchoolStatusRequest(@NotNull SchoolStatus status) {
}
