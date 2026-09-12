package com.projectx.people.dto;

import com.projectx.common.validation.Patterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InviteParentRequest(
        @NotBlank @Email String email,
        @Pattern(regexp = Patterns.PHONE, message = Patterns.PHONE_MESSAGE) String phone) {
}
