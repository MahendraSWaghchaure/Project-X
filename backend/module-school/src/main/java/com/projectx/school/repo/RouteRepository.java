package com.projectx.school.repo;

import com.projectx.school.domain.Route;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findBySchoolId(Long schoolId);

    /** Capped listing for the routes table. */
    List<Route> findBySchoolId(Long schoolId, Pageable pageable);

    /** Routes assigned to any of these vehicles (vehicle table's route-name column) - one query for the whole page. */
    List<Route> findByVehicleIdIn(Collection<Long> vehicleIds);

    long countBySchoolId(Long schoolId);
}
