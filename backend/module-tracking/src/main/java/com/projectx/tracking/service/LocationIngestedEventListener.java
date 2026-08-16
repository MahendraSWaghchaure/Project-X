package com.projectx.tracking.service;

import com.projectx.common.geo.GeoPoints;
import com.projectx.tracking.cache.CachedLocation;
import com.projectx.tracking.cache.LocationCacheService;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.event.LocationIngestedEvent;
import com.projectx.tracking.repo.TripRepository;
import com.projectx.tracking.ws.LiveLocationBroadcaster;
import org.locationtech.jts.geom.Point;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The actual work behind every ping, moved off the ingestion request thread - see
 * LocationIngestService for why. AFTER_COMMIT because the ping row must be durable before anyone
 * downstream (geofence dedup, the cache, a live map) acts on it.
 */
@Component
public class LocationIngestedEventListener {

    private final TripRepository tripRepository;
    private final LocationCacheService locationCacheService;
    private final LiveLocationBroadcaster broadcaster;
    private final GeofenceEvaluationService geofenceEvaluationService;
    private final SafetyFlagService safetyFlagService;

    public LocationIngestedEventListener(TripRepository tripRepository, LocationCacheService locationCacheService,
                                          LiveLocationBroadcaster broadcaster, GeofenceEvaluationService geofenceEvaluationService,
                                          SafetyFlagService safetyFlagService) {
        this.tripRepository = tripRepository;
        this.locationCacheService = locationCacheService;
        this.broadcaster = broadcaster;
        this.geofenceEvaluationService = geofenceEvaluationService;
        this.safetyFlagService = safetyFlagService;
    }

    // Deliberately no @Transactional here: GeofenceEvaluationService and SafetyFlagService each
    // manage their own writable transaction, and tripRepository.findById is self-transactional via
    // Spring Data. Wrapping this method in one (especially a readOnly one) would make their inner
    // inserts fail or silently join a read-only transaction.
    @Async("trackingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLocationIngested(LocationIngestedEvent event) {
        CachedLocation cached = new CachedLocation(event.vehicleId(), event.tripId(), event.latitude(), event.longitude(),
                event.speedKmh(), event.headingDegrees(), event.recordedAt());
        locationCacheService.put(cached);
        broadcaster.broadcast(cached);

        if (event.tripId() == null) {
            return;
        }
        Trip trip = tripRepository.findById(event.tripId()).orElse(null);
        if (trip == null) {
            return;
        }
        Point point = GeoPoints.toPoint(event.latitude(), event.longitude());
        geofenceEvaluationService.evaluate(trip, point);
        safetyFlagService.evaluate(trip, event.speedKmh(), event.recordedAt(), event.previousSpeedKmh(), event.previousRecordedAt());
    }
}
