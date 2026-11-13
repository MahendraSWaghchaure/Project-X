package com.projectx.identity.service;

import com.projectx.identity.domain.InvitationToken;
import com.projectx.identity.repo.InvitationTokenRepository;
import com.projectx.identity.security.TokenHasher;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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

    /** Resend: revokes any still-open invitation of this user, then issues a fresh one (only the newest link works). */
    public String reissueInvitation(Long userId) {
        invitationTokenRepository.expireOutstanding(userId, Instant.now());
        return issueInvitation(userId);
    }

    /**
     * Latest expiry of each user's not-yet-consumed invitation. Users absent from the map have none outstanding
     * (never invited, or already accepted). One query for the whole page - never per-user lookups.
     */
    @Transactional(readOnly = true)
    public Map<Long, Instant> latestOutstandingExpiry(Collection<Long> userIds) {
        Map<Long, Instant> out = new HashMap<>();
        if (userIds.isEmpty()) {
            return out;
        }
        for (Object[] row : invitationTokenRepository.latestOutstandingExpiry(userIds)) {
            out.put((Long) row[0], (Instant) row[1]);
        }
        return out;
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
