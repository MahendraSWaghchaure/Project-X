package com.projectx.school.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.geo.GeoPoints;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.school.domain.Route;
import com.projectx.school.domain.RouteStop;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.dto.AssignRouteRequest;
import com.projectx.school.dto.CreateRouteRequest;
import com.projectx.school.dto.RouteResponse;
import com.projectx.school.dto.RouteStopRequest;
import com.projectx.school.dto.RouteStopResponse;
import com.projectx.school.dto.UpdateRouteRequest;
import com.projectx.school.dto.UpdateRouteZoneRequest;
import com.projectx.school.repo.RouteRepository;
import com.projectx.school.repo.RouteStopRepository;
import com.projectx.school.repo.VehicleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RouteService {

    /** A school has tens of routes; the cap only bounds abuse. */
    static final int MAX_ROUTES_LISTED = 500;

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final VehicleRepository vehicleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    public RouteService(RouteRepository routeRepository, RouteStopRepository routeStopRepository,
                        VehicleRepository vehicleRepository, ApplicationEventPublisher eventPublisher) {
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.vehicleRepository = vehicleRepository;
        this.eventPublisher = eventPublisher;
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

    /** 1 query for routes (capped) + 1 for all their stops + 1 for the vehicles' registration numbers, regardless of how many routes a school has. */
    @Transactional(readOnly = true)
    public List<RouteResponse> listRoutes(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        List<Route> routes = routeRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_ROUTES_LISTED, Sort.by("name", "id")));
        List<Long> routeIds = routes.stream().map(Route::getId).toList();
        Map<Long, List<RouteStop>> stopsByRouteId = routeStopRepository
                .findByRouteIdInOrderByRouteIdAscSequenceNumberAsc(routeIds).stream()
                .collect(Collectors.groupingBy(RouteStop::getRouteId));
        Map<Long, String> vehicleNumbers = vehicleNumbers(routes);
        return routes.stream()
                .map(route -> RouteResponse.from(route,
                        toStopResponses(stopsByRouteId.getOrDefault(route.getId(), List.of())),
                        vehicleNumbers.get(route.getVehicleId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long schoolId, Long routeId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = findWithinSchool(schoolId, routeId);
        return detail(route);
    }

    public RouteResponse assignRoute(Long schoolId, Long routeId, AssignRouteRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = findWithinSchool(schoolId, routeId);
        route.setVehicleId(request.vehicleId());
        route.setDriverId(request.driverId());
        return detail(route);
    }

    public RouteResponse updateZone(Long schoolId, Long routeId, UpdateRouteZoneRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = findWithinSchool(schoolId, routeId);
        route.setZone(request.zoneMinRadiusMeters(), request.zoneMaxRadiusMeters());
        return detail(route);
    }

    /**
     * Edit a route and its ordered stops. Stops carrying the id of one of this route's stops are updated in place (so the
     * children assigned to them keep their stop); stops without an id are added; stops missing from the request are
     * removed - unless a child is still assigned to one of them, which is a 409 (re-assign the child first).
     */
    public RouteResponse updateRoute(Long schoolId, Long routeId, UpdateRouteRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = findWithinSchool(schoolId, routeId);
        route.setName(request.name());
        if (request.active() != null) {
            route.setActive(request.active());
        }
        if (request.zoneMinRadiusMeters() != null) {
            route.setZone(request.zoneMinRadiusMeters(), request.zoneMaxRadiusMeters());
        }

        Map<Long, RouteStop> existing = new HashMap<>();
        for (RouteStop stop : routeStopRepository.findByRouteIdOrderBySequenceNumberAsc(routeId)) {
            existing.put(stop.getId(), stop);
        }
        Set<Long> keptIds = new HashSet<>();
        List<RouteStop> toInsert = new ArrayList<>();
        for (RouteStopRequest r : request.stops()) {
            if (r.id() == null) {
                toInsert.add(toStop(routeId, r));
                continue;
            }
            RouteStop stop = existing.get(r.id());
            if (stop == null || !keptIds.add(r.id())) {
                throw new IllegalArgumentException("Unknown or duplicated stop id " + r.id() + " for this route");
            }
            stop.applyDetails(r.sequenceNumber(), r.label(), GeoPoints.toPoint(r.latitude(), r.longitude()),
                    r.radiusMeters() != null ? r.radiusMeters() : 150, r.scheduledTime());
        }
        List<Long> removed = existing.keySet().stream().filter(id -> !keptIds.contains(id)).toList();
        if (!removed.isEmpty()) {
            long inUse = ((Number) entityManager
                    .createNativeQuery("SELECT count(*) FROM children WHERE route_stop_id IN (:ids)")
                    .setParameter("ids", removed).getSingleResult()).longValue();
            if (inUse > 0) {
                throw new ConflictException("Cannot remove a stop that children are still assigned to - re-assign them first");
            }
            routeStopRepository.deleteAllById(removed);
        }
        routeStopRepository.saveAll(toInsert);
        routeStopRepository.flush();
        eventPublisher.publishEvent(AuditEvent.of("ROUTE_UPDATED", "ROUTE", routeId, schoolId,
                "stops=" + request.stops().size() + " active=" + route.isActive()));
        return detail(route);
    }

    private RouteResponse detail(Route route) {
        String vehicleNo = route.getVehicleId() == null ? null
                : vehicleRepository.findById(route.getVehicleId()).map(Vehicle::getRegistrationNumber).orElse(null);
        return RouteResponse.from(route, stopsFor(route.getId()), vehicleNo);
    }

    private Map<Long, String> vehicleNumbers(Collection<Route> routes) {
        List<Long> vehicleIds = routes.stream().map(Route::getVehicleId).filter(java.util.Objects::nonNull).distinct().toList();
        if (vehicleIds.isEmpty()) {
            return Map.of();
        }
        return vehicleRepository.findAllById(vehicleIds).stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getRegistrationNumber));
    }

    private List<RouteStopResponse> stopsFor(Long routeId) {
        return toStopResponses(routeStopRepository.findByRouteIdOrderBySequenceNumberAsc(routeId));
    }

    private List<RouteStopResponse> toStopResponses(List<RouteStop> stops) {
        return stops.stream().map(RouteStopResponse::from).toList();
    }

    private RouteStop toStop(Long routeId, RouteStopRequest request) {
        int radius = request.radiusMeters() != null ? request.radiusMeters() : 150;
        RouteStop stop = new RouteStop(routeId, request.sequenceNumber(), request.label(),
                GeoPoints.toPoint(request.latitude(), request.longitude()), radius);
        stop.setScheduledTime(request.scheduledTime());
        return stop;
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
