package com.projectx.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(@NotBlank @Size(min = 8) String newPassword) {
}
