package com.projectx.school.service;

import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.geo.GeoPoints;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.school.domain.Route;
import com.projectx.school.domain.RouteStop;
import com.projectx.school.dto.AssignRouteRequest;
import com.projectx.school.dto.CreateRouteRequest;
import com.projectx.school.dto.RouteResponse;
import com.projectx.school.dto.RouteStopRequest;
import com.projectx.school.dto.RouteStopResponse;
import com.projectx.school.dto.UpdateRouteZoneRequest;
import com.projectx.school.repo.RouteRepository;
import com.projectx.school.repo.RouteStopRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    public RouteService(RouteRepository routeRepository, RouteStopRepository routeStopRepository) {
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
    }

    public RouteResponse createRoute(Long schoolId, CreateRouteRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = new Route(schoolId, request.name());
        if (request.zoneMinRadiusMeters() != null) {
            route.setZone(request.zoneMinRadiusMeters(), request.zoneMaxRadiusMeters());
        }
        route = routeRepository.save(route);
        Long routeId = route.getId();
        List<RouteStop> stops = request.stops().stream()
                .map(s -> toStop(routeId, s))
                .toList();
        routeStopRepository.saveAll(stops);
        return RouteResponse.from(route, toStopResponses(stops));
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> listRoutes(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return routeRepository.findBySchoolId(schoolId).stream()
                .map(route -> RouteResponse.from(route, stopsFor(route.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long schoolId, Long routeId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = findWithinSchool(schoolId, routeId);
        return RouteResponse.from(route, stopsFor(route.getId()));
    }

    public RouteResponse assignRoute(Long schoolId, Long routeId, AssignRouteRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = findWithinSchool(schoolId, routeId);
        route.setVehicleId(request.vehicleId());
        route.setDriverId(request.driverId());
        return RouteResponse.from(route, stopsFor(route.getId()));
    }

    public RouteResponse updateZone(Long schoolId, Long routeId, UpdateRouteZoneRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = findWithinSchool(schoolId, routeId);
        route.setZone(request.zoneMinRadiusMeters(), request.zoneMaxRadiusMeters());
        return RouteResponse.from(route, stopsFor(route.getId()));
    }

    private List<RouteStopResponse> stopsFor(Long routeId) {
        return toStopResponses(routeStopRepository.findByRouteIdOrderBySequenceNumberAsc(routeId));
    }

    private List<RouteStopResponse> toStopResponses(List<RouteStop> stops) {
        return stops.stream().map(RouteStopResponse::from).toList();
    }

    private RouteStop toStop(Long routeId, RouteStopRequest request) {
        int radius = request.radiusMeters() != null ? request.radiusMeters() : 150;
        return new RouteStop(routeId, request.sequenceNumber(), request.label(),
                GeoPoints.toPoint(request.latitude(), request.longitude()), radius);
    }

    private Route findWithinSchool(Long schoolId, Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> ResourceNotFoundException.of("Route", routeId));
        if (!route.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Route", routeId);
        }
        return route;
    }
}
