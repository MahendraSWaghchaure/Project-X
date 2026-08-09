package com.projectx.people.dto;

import jakarta.validation.constraints.NotBlank;

/** Called after the parent has already accepted their invite (see module-identity's /auth/invitations accept flow, which handles setting a password and activating the account). */
public record CompleteParentProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        @NotBlank String address,
        Double latitude,
        Double longitude) {
}
