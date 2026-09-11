package com.projectx.tracking.service;

import com.projectx.common.events.GeofenceEventType;
import com.projectx.common.events.GeofenceRaisedEvent;
import com.projectx.common.geo.GeoPoints;
import com.projectx.people.repo.ChildRepository;
import com.projectx.tracking.domain.GeofenceEvent;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.repo.GeofenceEventRepository;
import com.projectx.tracking.service.GeoLookupCacheService.StopGeoInfo;
import java.time.Instant;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs on every ingested ping for a trip in progress. "Approaching" fires at
 * APPROACHING_MULTIPLIER x the arrival radius so parents get a heads-up before the vehicle is
 * already there; each event type fires at most once per trip (school-wide) or once per
 * child-per-trip (per-stop), enforced via GeofenceEventRepository lookups before insert. School
 * location and route-stop lookups go through GeoLookupCacheService rather than the repositories
 * directly - this runs on every ping, the busiest read path in the app.
 */
@Service
@Transactional
public class GeofenceEvaluationService {

    private static final int APPROACHING_MULTIPLIER = 3;

    private final GeoLookupCacheService geoLookupCacheService;
    private final ChildRepository childRepository;
    private final GeofenceEventRepository geofenceEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GeofenceEventKeySeenFilter keySeenFilter;

    public GeofenceEvaluationService(GeoLookupCacheService geoLookupCacheService, ChildRepository childRepository,
                                      GeofenceEventRepository geofenceEventRepository,
                                      ApplicationEventPublisher eventPublisher, GeofenceEventKeySeenFilter keySeenFilter) {
        this.geoLookupCacheService = geoLookupCacheService;
        this.childRepository = childRepository;
        this.geofenceEventRepository = geofenceEventRepository;
        this.eventPublisher = eventPublisher;
        this.keySeenFilter = keySeenFilter;
    }

    public void evaluate(Trip trip, Point currentLocation) {
        evaluateSchoolGeofence(trip, currentLocation);
        evaluateStopGeofences(trip, currentLocation);
    }

    private void evaluateSchoolGeofence(Trip trip, Point currentLocation) {
        var schoolGeoInfo = geoLookupCacheService.getSchoolGeoInfo(trip.getSchoolId()).orElse(null);
        if (schoolGeoInfo == null) {
            return;
        }
        Point schoolLocation = GeoPoints.toPoint(schoolGeoInfo.location());
        double distance = GeoPoints.distanceMeters(schoolLocation, currentLocation);
        if (distance <= schoolGeoInfo.geofenceRadiusMeters()) {
            raiseSchoolWideOnce(trip, GeofenceEventType.ARRIVED_SCHOOL);
        } else if (distance <= schoolGeoInfo.geofenceRadiusMeters() * (double) APPROACHING_MULTIPLIER) {
            raiseSchoolWideOnce(trip, GeofenceEventType.APPROACHING_SCHOOL);
        }
    }

    private void evaluateStopGeofences(Trip trip, Point currentLocation) {
        for (StopGeoInfo stop : geoLookupCacheService.getStopGeoInfo(trip.getRouteId())) {
            Point stopLocation = GeoPoints.toPoint(stop.location());
            double distance = GeoPoints.distanceMeters(stopLocation, currentLocation);
            GeofenceEventType type;
            if (distance <= stop.radiusMeters()) {
                type = GeofenceEventType.ARRIVED_STOP;
            } else if (distance <= stop.radiusMeters() * (double) APPROACHING_MULTIPLIER) {
                type = GeofenceEventType.APPROACHING_STOP;
            } else {
                continue;
            }
            childRepository.findByRouteStopId(stop.id())
                    .forEach(child -> raisePerChildOnce(trip, stop.id(), child.getId(), type));
        }
    }

    public void raiseEndOfDayDelay(Trip trip) {
        raiseSchoolWideOnce(trip, GeofenceEventType.END_OF_DAY_DELAY);
    }

    private void raiseSchoolWideOnce(Trip trip, GeofenceEventType type) {
        String key = "school:" + trip.getId() + ":" + type;
        if (!keySeenFilter.definitelyNotSeenBefore(key)
                && geofenceEventRepository.existsByTripIdAndTypeAndChildIdIsNull(trip.getId(), type)) {
            return;
        }
        persistAndPublish(trip, null, null, type);
    }

    private void raisePerChildOnce(Trip trip, Long routeStopId, Long childId, GeofenceEventType type) {
        String key = "child:" + trip.getId() + ":" + type + ":" + childId;
        if (!keySeenFilter.definitelyNotSeenBefore(key)
                && geofenceEventRepository.existsByTripIdAndTypeAndChildId(trip.getId(), type, childId)) {
            return;
        }
        persistAndPublish(trip, routeStopId, childId, type);
    }

    private void persistAndPublish(Trip trip, Long routeStopId, Long childId, GeofenceEventType type) {
        geofenceEventRepository.save(new GeofenceEvent(trip.getId(), trip.getVehicleId(), trip.getSchoolId(), routeStopId, childId, type));
        eventPublisher.publishEvent(new GeofenceRaisedEvent(
                trip.getId(), trip.getVehicleId(), trip.getSchoolId(), trip.getRouteId(), routeStopId, childId, type, Instant.now()));
    }
}
