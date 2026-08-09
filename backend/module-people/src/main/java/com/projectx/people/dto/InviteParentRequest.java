package com.projectx.people.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteParentRequest(@NotBlank @Email String email, String phone) {
}
