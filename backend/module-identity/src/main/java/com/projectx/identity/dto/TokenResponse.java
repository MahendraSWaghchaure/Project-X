package com.projectx.identity.dto;

import com.projectx.common.security.Role;

public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds, Role role) {
}
