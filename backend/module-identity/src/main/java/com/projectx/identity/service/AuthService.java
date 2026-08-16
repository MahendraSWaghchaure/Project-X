package com.projectx.identity.service;

import com.projectx.identity.domain.PasswordResetToken;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.domain.UserStatus;
import com.projectx.identity.dto.LoginRequest;
import com.projectx.identity.dto.TokenResponse;
import com.projectx.identity.repo.PasswordResetTokenRepository;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.identity.security.TokenHasher;
import com.projectx.identity.security.TokenService;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofHours(1);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final InvitationService invitationService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
                        TokenService tokenService, InvitationService invitationService,
                        PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.invitationService = invitationService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
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

    /**
     * Deliberately silent either way (same response whether or not the email is registered) -
     * telling an anonymous caller "that email doesn't exist" is a user-enumeration leak. The
     * actual reset link is never returned from this endpoint, only logged - SMTP delivery is off
     * by default in this environment (see backend/README.md's "Payments" section for the same
     * off-by-default-and-honest-about-it pattern applied to notifications), and unlike the invite
     * flow, the caller here isn't an authenticated admin acting on someone else's behalf - it's
     * an anonymous request, so the token can't be handed back in the HTTP response without
     * defeating the entire point of "prove you own this inbox."
     */
    public void requestPasswordReset(String email) {
        userAccountRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = TokenHasher.newOpaqueToken();
            passwordResetTokenRepository.save(
                    new PasswordResetToken(user.getId(), TokenHasher.sha256(rawToken), Instant.now().plus(PASSWORD_RESET_TTL)));
            // A real deployment would email this link (see DEPLOY.md for wiring up real SMTP)
            // instead of just logging it - logged here so local/dev testing without SMTP
            // configured still has a way to complete the flow, without ever putting the token in
            // an HTTP response an anonymous caller could read.
            log.info("Password reset requested for {} - reset link token (dev-only, would be emailed in prod): {}",
                    email, rawToken);
        });
    }

    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired reset link"));
        UserAccount user = userAccountRepository.findById(token.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired reset link"));

        user.changePassword(passwordEncoder.encode(newPassword));
        token.consume();
        tokenService.revokeAllForUser(user.getId());
    }
}
