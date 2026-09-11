package com.projectx.tracking.service;

import com.projectx.common.events.SchoolLocationChangedEvent;
import com.projectx.common.geo.GeoPoint;
import com.projectx.common.geo.GeoPoints;
import com.projectx.school.repo.RouteStopRepository;
import com.projectx.school.repo.SchoolRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caches the two lookups GeofenceEvaluationService makes on every ingested ping - a school's
 * location/geofence radius, and a route's stop list - both of which change essentially never in
 * practice. Returns plain GeoPoint-based records rather than the JPA entities themselves: a JTS
 * Point field doesn't serialize cleanly into Redis by default, and the caller only ever needs
 * lat/lng + a radius anyway.
 *
 * <p>Route stops have no edit endpoint in this codebase today (RouteController only creates them
 * as part of route creation) - a route's stop list literally cannot change after creation, so
 * there's nothing to evict there. School location/radius CAN change (SchoolService.updateSchool),
 * so that side listens for SchoolLocationChangedEvent and evicts explicitly rather than relying on
 * the cache's TTL alone (see TrackingCacheConfig) to bound staleness.
 */
@Service
@Transactional(readOnly = true)
public class GeoLookupCacheService {

    private final SchoolRepository schoolRepository;
    private final RouteStopRepository routeStopRepository;

    public GeoLookupCacheService(SchoolRepository schoolRepository, RouteStopRepository routeStopRepository) {
        this.schoolRepository = schoolRepository;
        this.routeStopRepository = routeStopRepository;
    }

    public record SchoolGeoInfo(GeoPoint location, int geofenceRadiusMeters) {}

    public record StopGeoInfo(Long id, GeoPoint location, int radiusMeters) {}

    @Cacheable(value = "schoolGeoInfo", key = "#schoolId")
    public Optional<SchoolGeoInfo> getSchoolGeoInfo(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .filter(school -> school.getLocation() != null)
                .map(school -> new SchoolGeoInfo(GeoPoints.toGeoPoint(school.getLocation()), school.getGeofenceRadiusMeters()));
    }

    @Cacheable(value = "routeStopGeoInfo", key = "#routeId")
    public List<StopGeoInfo> getStopGeoInfo(Long routeId) {
        return routeStopRepository.findByRouteIdOrderBySequenceNumberAsc(routeId).stream()
                .map(stop -> new StopGeoInfo(stop.getId(), GeoPoints.toGeoPoint(stop.getLocation()), stop.getRadiusMeters()))
                .toList();
    }

    @CacheEvict(value = "schoolGeoInfo", key = "#event.schoolId()")
    @EventListener
    public void onSchoolLocationChanged(SchoolLocationChangedEvent event) {
        // Body intentionally empty - @CacheEvict does the work; this just needs to be a real
        // Spring-proxied method the event triggers.
    }
}
