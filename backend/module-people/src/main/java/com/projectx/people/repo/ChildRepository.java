package com.projectx.people.repo;

import com.projectx.people.domain.Child;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, Long> {

    List<Child> findBySchoolId(Long schoolId);

    List<Child> findByPrimaryParentIdOrSecondaryParentId(Long primaryParentId, Long secondaryParentId);

    List<Child> findByRouteId(Long routeId);

    List<Child> findByRouteStopId(Long routeStopId);
}
