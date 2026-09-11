package com.projectx.school.repo;

import com.projectx.school.domain.RouteStop;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByRouteIdOrderBySequenceNumberAsc(Long routeId);

    /** Batch-fetches stops for every route in one query - see RouteService.listRoutes, which used to do this one route at a time. */
    List<RouteStop> findByRouteIdInOrderByRouteIdAscSequenceNumberAsc(Collection<Long> routeIds);

    void deleteByRouteId(Long routeId);
}
