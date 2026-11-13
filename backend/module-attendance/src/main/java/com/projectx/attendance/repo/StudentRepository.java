package com.projectx.attendance.repo;

import com.projectx.attendance.domain.Student;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByIdAndSchoolId(Long id, Long schoolId);

    Optional<Student> findByChildId(Long childId);

    /** Import dedup lookup - bounded by the import's own 500-row cap. */
    List<Student> findBySchoolIdAndRollNoIn(Long schoolId, Collection<String> rollNos);

    /** Platform-admin leads: only guardians who consented, only students NOT already Sunju customers. Backed by the partial index idx_students_leads. */
    Page<Student> findByMarketingConsentTrueAndChildIdIsNull(Pageable pageable);

    /** Whole roster size - indexed count, no row fetch (B1 school overview). */
    long countBySchoolId(Long schoolId);

    /** Roster rows already linked to a Sunju-transport child (B1 school overview's "subscribed" count). */
    long countBySchoolIdAndChildIdIsNotNull(Long schoolId);

    /** Records a guardian's answer (bumps the optimistic-lock version so a concurrent roster edit is detected). */
    @Modifying
    @Query("update Student s set s.marketingConsent = :consent, s.consentAt = :at, s.consentSource = :source, s.version = s.version + 1 "
            + "where s.id = :id")
    int applyConsent(@Param("id") Long id, @Param("consent") boolean consent, @Param("at") Instant at, @Param("source") String source);
}
