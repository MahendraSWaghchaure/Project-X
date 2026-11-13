package com.projectx.people.repo;

import com.projectx.people.domain.Child;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChildRepository extends JpaRepository<Child, Long> {

    List<Child> findBySchoolId(Long schoolId);

    /** Paginated variant for the school-admin-facing listing - a large school can have hundreds of children. Overloaded on Pageable, not a rename, so existing internal unbounded callers (fee invoicing, absence checks) are untouched. */
    Page<Child> findBySchoolId(Long schoolId, Pageable pageable);

    /** Every child this parent is a guardian of - primary directly, or via any number of child_guardians rows. */
    @Query("select c from Child c where c.primaryParentId = :parentId "
            + "or c.id in (select g.childId from ChildGuardian g where g.parentId = :parentId)")
    List<Child> findByGuardianParentId(@Param("parentId") Long parentId);

    List<Child> findByRouteId(Long routeId);

    List<Child> findByRouteStopId(Long routeStopId);

    long countBySchoolId(Long schoolId);

    /**
     * Rows of (vehicleId, activeChildCount): how many active children ride a route that is assigned to each vehicle.
     * One aggregate query for a whole vehicles page (cross-module join by id, no FK needed).
     */
    @Query("select r.vehicleId, count(c.id) from Child c, com.projectx.school.domain.Route r "
            + "where c.routeId = r.id and r.vehicleId in :vehicleIds and c.status = com.projectx.people.domain.ChildStatus.ACTIVE "
            + "group by r.vehicleId")
    List<Object[]> countActiveByVehicle(@Param("vehicleIds") Collection<Long> vehicleIds);
}
