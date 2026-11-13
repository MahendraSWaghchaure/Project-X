package com.projectx.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterDeviceTokenRequest(@NotBlank String token) {
}
