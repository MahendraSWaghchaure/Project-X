package com.projectx.identity.service;

import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.domain.UserStatus;
import com.projectx.identity.dto.LoginRequest;
import com.projectx.identity.dto.TokenResponse;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.identity.security.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final InvitationService invitationService;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
                        TokenService tokenService, InvitationService invitationService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.invitationService = invitationService;
    }

    public TokenResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!user.isActive()) {
            throw new DisabledException("Account is " + user.getStatus());
        }

        TokenService.IssuedTokens tokens = tokenService.issueTokens(user);
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds(), user.getRole());
    }

    public TokenResponse refresh(String rawRefreshToken) {
        Long userId = tokenService.consumeRefreshToken(rawRefreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
        if (!user.isActive()) {
            throw new DisabledException("Account is " + user.getStatus());
        }

        TokenService.IssuedTokens tokens = tokenService.issueTokens(user);
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds(), user.getRole());
    }

    public void logout(Long userId) {
        tokenService.revokeAllForUser(userId);
    }

    /** Consumes an invite token, sets the chosen password, activates the account, and logs the user straight in. */
    public TokenResponse acceptInvitation(String rawInvitationToken, String newPassword) {
        Long userId = invitationService.consumeInvitation(rawInvitationToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired invitation"));
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired invitation"));

        user.changePassword(passwordEncoder.encode(newPassword));
        user.setStatus(UserStatus.ACTIVE);

        TokenService.IssuedTokens tokens = tokenService.issueTokens(user);
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds(), user.getRole());
    }
}
