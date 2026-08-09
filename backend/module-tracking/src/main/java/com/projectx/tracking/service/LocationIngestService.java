package com.projectx.tracking.service;

import com.projectx.common.geo.GeoPoints;
import com.projectx.school.domain.Vehicle;
import com.projectx.tracking.cache.CachedLocation;
import com.projectx.tracking.cache.LocationCacheService;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.domain.TripStatus;
import com.projectx.tracking.domain.VehicleLocationPing;
import com.projectx.tracking.repo.TripRepository;
import com.projectx.tracking.repo.VehicleLocationPingRepository;
import com.projectx.tracking.ws.LiveLocationBroadcaster;
import java.time.Instant;
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The single entry point every ping - Traccar webhook today, hardware-native ingestion later - funnels through. */
@Service
@Transactional
public class LocationIngestService {

    private final TripRepository tripRepository;
    private final VehicleLocationPingRepository pingRepository;
    private final LocationCacheService locationCacheService;
    private final LiveLocationBroadcaster broadcaster;
    private final GeofenceEvaluationService geofenceEvaluationService;

    public LocationIngestService(TripRepository tripRepository, VehicleLocationPingRepository pingRepository,
                                  LocationCacheService locationCacheService, LiveLocationBroadcaster broadcaster,
                                  GeofenceEvaluationService geofenceEvaluationService) {
        this.tripRepository = tripRepository;
        this.pingRepository = pingRepository;
        this.locationCacheService = locationCacheService;
        this.broadcaster = broadcaster;
        this.geofenceEvaluationService = geofenceEvaluationService;
    }

    public void recordPing(Vehicle vehicle, double latitude, double longitude, Double speedKmh, Double headingDegrees, Instant recordedAt) {
        Trip activeTrip = tripRepository
                .findByVehicleIdAndStatusIn(vehicle.getId(), List.of(TripStatus.IN_PROGRESS, TripStatus.DELAYED))
                .orElse(null);
        Point point = GeoPoints.toPoint(latitude, longitude);
        Long tripId = activeTrip != null ? activeTrip.getId() : null;

        pingRepository.save(new VehicleLocationPing(vehicle.getId(), tripId, point, speedKmh, headingDegrees, recordedAt));

        CachedLocation cached = new CachedLocation(vehicle.getId(), tripId, latitude, longitude, speedKmh, headingDegrees, recordedAt);
        locationCacheService.put(cached);
        broadcaster.broadcast(cached);

        if (activeTrip != null) {
            geofenceEvaluationService.evaluate(activeTrip, point);
        }
    }
}
