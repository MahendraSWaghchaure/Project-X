package com.projectx.attendance.repo;

import com.projectx.attendance.domain.TeacherProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    Optional<TeacherProfile> findByUserId(Long userId);

    /** Profiles of a bounded page of teachers / class teachers - one query. */
    List<TeacherProfile> findByUserIdIn(Collection<Long> userIds);
}
