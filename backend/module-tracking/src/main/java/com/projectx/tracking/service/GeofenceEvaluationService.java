package com.projectx.tracking.service;

import com.projectx.common.events.GeofenceEventType;
import com.projectx.common.events.GeofenceRaisedEvent;
import com.projectx.common.geo.GeoPoints;
import com.projectx.people.repo.ChildRepository;
import com.projectx.school.domain.RouteStop;
import com.projectx.school.domain.School;
import com.projectx.school.repo.RouteStopRepository;
import com.projectx.school.repo.SchoolRepository;
import com.projectx.tracking.domain.GeofenceEvent;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.repo.GeofenceEventRepository;
import java.time.Instant;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs on every ingested ping for a trip in progress. "Approaching" fires at
 * APPROACHING_MULTIPLIER x the arrival radius so parents get a heads-up before the vehicle is
 * already there; each event type fires at most once per trip (school-wide) or once per
 * child-per-trip (per-stop), enforced via GeofenceEventRepository lookups before insert.
 */
@Service
@Transactional
public class GeofenceEvaluationService {

    private static final int APPROACHING_MULTIPLIER = 3;

    private final SchoolRepository schoolRepository;
    private final RouteStopRepository routeStopRepository;
    private final ChildRepository childRepository;
    private final GeofenceEventRepository geofenceEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GeofenceEventKeySeenFilter keySeenFilter;

    public GeofenceEvaluationService(SchoolRepository schoolRepository, RouteStopRepository routeStopRepository,
                                      ChildRepository childRepository, GeofenceEventRepository geofenceEventRepository,
                                      ApplicationEventPublisher eventPublisher, GeofenceEventKeySeenFilter keySeenFilter) {
        this.schoolRepository = schoolRepository;
        this.routeStopRepository = routeStopRepository;
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
        School school = schoolRepository.findById(trip.getSchoolId()).orElse(null);
        if (school == null || school.getLocation() == null) {
            return;
        }
        double distance = GeoPoints.distanceMeters(school.getLocation(), currentLocation);
        if (distance <= school.getGeofenceRadiusMeters()) {
            raiseSchoolWideOnce(trip, GeofenceEventType.ARRIVED_SCHOOL);
        } else if (distance <= school.getGeofenceRadiusMeters() * (double) APPROACHING_MULTIPLIER) {
            raiseSchoolWideOnce(trip, GeofenceEventType.APPROACHING_SCHOOL);
        }
    }

    private void evaluateStopGeofences(Trip trip, Point currentLocation) {
        for (RouteStop stop : routeStopRepository.findByRouteIdOrderBySequenceNumberAsc(trip.getRouteId())) {
            double distance = GeoPoints.distanceMeters(stop.getLocation(), currentLocation);
            GeofenceEventType type;
            if (distance <= stop.getRadiusMeters()) {
                type = GeofenceEventType.ARRIVED_STOP;
            } else if (distance <= stop.getRadiusMeters() * (double) APPROACHING_MULTIPLIER) {
                type = GeofenceEventType.APPROACHING_STOP;
            } else {
                continue;
            }
            childRepository.findByRouteStopId(stop.getId())
                    .forEach(child -> raisePerChildOnce(trip, stop.getId(), child.getId(), type));
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
