package com.projectx.school.repo;

import com.projectx.school.domain.SchoolAdminProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolAdminProfileRepository extends JpaRepository<SchoolAdminProfile, Long> {

    Optional<SchoolAdminProfile> findByUserId(Long userId);

    /** Profiles for a bounded set of user ids (display-name lookups) - one query. */
    List<SchoolAdminProfile> findByUserIdIn(java.util.Collection<Long> userIds);

    List<SchoolAdminProfile> findBySchoolId(Long schoolId);
}
