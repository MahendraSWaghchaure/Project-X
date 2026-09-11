package com.projectx.people.repo;

import com.projectx.people.domain.Child;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, Long> {

    List<Child> findBySchoolId(Long schoolId);

    /** Paginated variant for the school-admin-facing listing - a large school can have hundreds of children. Overloaded on Pageable, not a rename, so existing internal unbounded callers (fee invoicing, absence checks) are untouched. */
    Page<Child> findBySchoolId(Long schoolId, Pageable pageable);

    List<Child> findByPrimaryParentIdOrSecondaryParentId(Long primaryParentId, Long secondaryParentId);

    List<Child> findByRouteId(Long routeId);

    List<Child> findByRouteStopId(Long routeStopId);
}
