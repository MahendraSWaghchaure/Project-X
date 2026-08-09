package com.projectx.school.repo;

import com.projectx.school.domain.RouteStop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByRouteIdOrderBySequenceNumberAsc(Long routeId);

    void deleteByRouteId(Long routeId);
}
