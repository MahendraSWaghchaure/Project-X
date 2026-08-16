package com.projectx.identity.web;

import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.web.ApiResponse;
import com.projectx.identity.dto.AcceptInvitationRequest;
import com.projectx.identity.dto.ForgotPasswordRequest;
import com.projectx.identity.dto.LoginRequest;
import com.projectx.identity.dto.RefreshRequest;
import com.projectx.identity.dto.ResetPasswordRequest;
import com.projectx.identity.dto.TokenResponse;
import com.projectx.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(CurrentUserContext.get().userId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/invitations/{token}/accept")
    public ApiResponse<TokenResponse> acceptInvitation(@PathVariable String token,
                                                         @Valid @RequestBody AcceptInvitationRequest request) {
        return ApiResponse.ok(authService.acceptInvitation(token, request.newPassword()));
    }

    /** Always the same response whether or not the email is registered - see AuthService.requestPasswordReset. */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return ApiResponse.ok(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.ok(null);
    }
}
