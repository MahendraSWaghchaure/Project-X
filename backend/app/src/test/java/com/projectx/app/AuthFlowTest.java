package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.identity.dto.LoginRequest;
import com.projectx.identity.dto.RefreshRequest;
import com.projectx.identity.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthFlowTest extends IntegrationTestBase {

    @Test
    void loginWithBootstrapPlatformAdminSucceeds() {
        ResponseEntity<ApiResponse<TokenResponse>> response = exchange(
                "/auth/login", HttpMethod.POST, new LoginRequest(PLATFORM_ADMIN_EMAIL, PLATFORM_ADMIN_PASSWORD), null,
                new ParameterizedTypeReference<>() {});

        assertSuccess(response);
        assertThat(response.getBody().data().accessToken()).isNotBlank();
        assertThat(response.getBody().data().refreshToken()).isNotBlank();
        assertThat(response.getBody().data().role().name()).isEqualTo("PLATFORM_ADMIN");
    }

    @Test
    void loginWithWrongPasswordReturns401NotStackTrace() {
        ResponseEntity<ApiResponse<TokenResponse>> response = exchange(
                "/auth/login", HttpMethod.POST, new LoginRequest(PLATFORM_ADMIN_EMAIL, "definitely-wrong"), null,
                new ParameterizedTypeReference<>() {});

        assertStatus(response, HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void loginWithUnknownEmailReturns401NotServerError() {
        ResponseEntity<ApiResponse<TokenResponse>> response = exchange(
                "/auth/login", HttpMethod.POST, new LoginRequest(unique("nobody") + "@test.example", "whatever123"), null,
                new ParameterizedTypeReference<>() {});

        assertStatus(response, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshTokenRotatesAndReturnsAWorkingAccessToken() {
        ResponseEntity<ApiResponse<TokenResponse>> loginResponse = exchange(
                "/auth/login", HttpMethod.POST, new LoginRequest(PLATFORM_ADMIN_EMAIL, PLATFORM_ADMIN_PASSWORD), null,
                new ParameterizedTypeReference<>() {});
        assertSuccess(loginResponse);
        String refreshToken = loginResponse.getBody().data().refreshToken();

        ResponseEntity<ApiResponse<TokenResponse>> refreshResponse = exchange(
                "/auth/refresh", HttpMethod.POST, new RefreshRequest(refreshToken), null,
                new ParameterizedTypeReference<>() {});

        assertSuccess(refreshResponse);
        assertThat(refreshResponse.getBody().data().accessToken()).isNotBlank();
    }

    @Test
    void anEndpointRequiringAuthRejectsRequestsWithNoToken() {
        ResponseEntity<ApiResponse<Object>> response = exchange(
                "/schools", HttpMethod.GET, null, null, new ParameterizedTypeReference<>() {});

        assertStatus(response, HttpStatus.UNAUTHORIZED);
    }
}
