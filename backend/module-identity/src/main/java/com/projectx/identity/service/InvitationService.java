package com.projectx.identity.service;

import com.projectx.identity.domain.InvitationToken;
import com.projectx.identity.repo.InvitationTokenRepository;
import com.projectx.identity.security.TokenHasher;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvitationService {

    private static final Duration INVITATION_TTL = Duration.ofDays(7);

    private final InvitationTokenRepository invitationTokenRepository;

    public InvitationService(InvitationTokenRepository invitationTokenRepository) {
        this.invitationTokenRepository = invitationTokenRepository;
    }

    /** Returns the raw token - hand it to the caller to embed in an invite link; only its hash is persisted. */
    public String issueInvitation(Long userId) {
        String rawToken = TokenHasher.newOpaqueToken();
        invitationTokenRepository.save(
                new InvitationToken(userId, TokenHasher.sha256(rawToken), Instant.now().plus(INVITATION_TTL)));
        return rawToken;
    }

    public Optional<Long> consumeInvitation(String rawToken) {
        return invitationTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .filter(InvitationToken::isValid)
                .map(token -> {
                    token.consume();
                    return token.getUserId();
                });
    }
}
