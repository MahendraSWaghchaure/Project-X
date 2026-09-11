package com.projectx.school.repo;

import com.projectx.school.domain.School;
import com.projectx.school.domain.SchoolStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
    long countByStatus(SchoolStatus status);
}
