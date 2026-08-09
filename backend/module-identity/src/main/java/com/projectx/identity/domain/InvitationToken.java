package com.projectx.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** Lets an INVITED user (a parent invited by a school admin) set their own password and activate, without ever having had a password to log in with. */
@Entity
@Table(name = "invitation_tokens", indexes = {
        @Index(name = "idx_invitation_tokens_token_hash", columnList = "token_hash", unique = true)
})
public class InvitationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed", nullable = false)
    private boolean consumed = false;

    protected InvitationToken() {
    }

    public InvitationToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isValid() {
        return !consumed && expiresAt.isAfter(Instant.now());
    }

    public void consume() {
        this.consumed = true;
    }
}
