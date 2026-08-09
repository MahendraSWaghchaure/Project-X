package com.projectx.people.repo;

import com.projectx.people.domain.ParentProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentProfileRepository extends JpaRepository<ParentProfile, Long> {

    Optional<ParentProfile> findByUserId(Long userId);
}
