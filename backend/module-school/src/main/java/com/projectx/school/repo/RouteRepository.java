package com.projectx.school.repo;

import com.projectx.school.domain.Route;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findBySchoolId(Long schoolId);

    long countBySchoolId(Long schoolId);
}
