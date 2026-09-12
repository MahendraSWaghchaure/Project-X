package com.projectx.people.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Called after the parent has already accepted their invite (see module-identity's /auth/invitations accept flow, which handles setting a password and activating the account). */
public record CompleteParentProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone,
        @NotBlank String address,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude) {
}
