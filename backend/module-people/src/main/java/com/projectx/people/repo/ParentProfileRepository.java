package com.projectx.people.repo;

import com.projectx.people.domain.ParentProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentProfileRepository extends JpaRepository<ParentProfile, Long> {

    Optional<ParentProfile> findByUserId(Long userId);

    /** Profiles for a bounded set of user ids (display-name lookups) - one query. */
    List<ParentProfile> findByUserIdIn(java.util.Collection<Long> userIds);

    List<ParentProfile> findByInvitedBySchoolId(Long schoolId);
}
