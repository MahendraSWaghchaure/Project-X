package com.projectx.school.repo;

import com.projectx.school.domain.School;
import com.projectx.school.domain.SchoolStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
    long countByStatus(SchoolStatus status);

    /** Backs the platform-admin location filter - the frontend resolves a Country/State/Region selection down to a set of district ids (via /geo/districts) before calling this, so module-school never needs to depend on module-geo itself. */
    List<School> findByDistrictIdIn(Collection<Long> districtIds);
}
