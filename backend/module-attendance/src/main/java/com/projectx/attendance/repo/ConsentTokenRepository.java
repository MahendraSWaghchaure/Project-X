package com.projectx.attendance.repo;

import com.projectx.attendance.domain.ConsentToken;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentTokenRepository extends JpaRepository<ConsentToken, Long> {

    Optional<ConsentToken> findByTokenHash(String tokenHash);

    /** The newest request of each of these students (for a roster page) - one query. */
    @Query("select t from ConsentToken t where t.id in "
            + "(select max(t2.id) from ConsentToken t2 where t2.studentId in :studentIds group by t2.studentId)")
    List<ConsentToken> findLatestForStudents(@Param("studentIds") Collection<Long> studentIds);

    /** How many links were issued for this student since {@code since} (spam / SMS-cost guard). */
    long countByStudentIdAndCreatedAtAfter(Long studentId, Instant since);

    /**
     * Single-use, atomic: marks the token used ONLY if it is still unused and unexpired. Returns 1 for exactly one caller
     * even under concurrent requests (the row lock serializes them; the loser re-evaluates used_at and matches nothing).
     */
    @Modifying
    @Query("update ConsentToken t set t.usedAt = :now, t.response = :response "
            + "where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now")
    int markUsed(@Param("hash") String hash, @Param("response") boolean response, @Param("now") Instant now);

    /** Revokes every still-open link of a student before issuing a fresh one (only the newest link works). */
    @Modifying
    @Query("update ConsentToken t set t.expiresAt = :now where t.studentId = :studentId and t.usedAt is null and t.expiresAt > :now")
    int expireOutstanding(@Param("studentId") Long studentId, @Param("now") Instant now);
}
