package com.projectx.identity.repo;

import com.projectx.identity.domain.InvitationToken;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, Long> {

    Optional<InvitationToken> findByTokenHash(String tokenHash);

    /** Latest expiry among each user's not-yet-consumed invitations: rows are (userId, expiresAt). A user with no row has no outstanding invitation. */
    @Query("select t.userId, max(t.expiresAt) from InvitationToken t where t.userId in :userIds and t.consumed = false group by t.userId")
    List<Object[]> latestOutstandingExpiry(@Param("userIds") Collection<Long> userIds);

    /** Revokes every still-open invitation of a user (used before re-issuing, so only the newest link works). */
    @Modifying
    @Query("update InvitationToken t set t.expiresAt = :now where t.userId = :userId and t.consumed = false and t.expiresAt > :now")
    int expireOutstanding(@Param("userId") Long userId, @Param("now") Instant now);
}
