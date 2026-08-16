package com.projectx.tracking.service;

import com.projectx.common.geo.GeoPoints;
import com.projectx.school.domain.Vehicle;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.domain.TripStatus;
import com.projectx.tracking.domain.VehicleLocationPing;
import com.projectx.tracking.event.LocationIngestedEvent;
import com.projectx.tracking.repo.TripRepository;
import com.projectx.tracking.repo.VehicleLocationPingRepository;
import java.time.Instant;
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single entry point every ping - Traccar webhook today, hardware-native ingestion later -
 * funnels through. Deliberately does only the minimum synchronous work (resolve the active trip,
 * persist the raw ping) before publishing an event and returning - geofence checks, safety-flag
 * checks, the cache update, and the live broadcast all happen off this thread, in
 * LocationIngestedEventListener. This is the seam a Kafka consumer occupies once ingestion needs
 * to scale past what one instance's request threads can absorb (see backend/ARCHITECTURE.md) -
 * the ingest endpoint's job is "accept and persist fast," nothing else.
 */
@Service
@Transactional
public class LocationIngestService {

    private final TripRepository tripRepository;
    private final VehicleLocationPingRepository pingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LocationIngestService(TripRepository tripRepository, VehicleLocationPingRepository pingRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.tripRepository = tripRepository;
        this.pingRepository = pingRepository;
        this.eventPublisher = eventPublisher;
    }

    public void recordPing(Vehicle vehicle, double latitude, double longitude, Double speedKmh, Double headingDegrees, Instant recordedAt) {
        Trip activeTrip = tripRepository
                .findByVehicleIdAndStatusIn(vehicle.getId(), List.of(TripStatus.IN_PROGRESS, TripStatus.DELAYED))
                .orElse(null);
        VehicleLocationPing previousPing = pingRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicle.getId()).orElse(null);
        Point point = GeoPoints.toPoint(latitude, longitude);
        Long tripId = activeTrip != null ? activeTrip.getId() : null;

        pingRepository.save(new VehicleLocationPing(vehicle.getId(), tripId, point, speedKmh, headingDegrees, recordedAt));

        eventPublisher.publishEvent(new LocationIngestedEvent(vehicle.getId(), tripId, latitude, longitude, speedKmh, headingDegrees,
                recordedAt, previousPing != null ? previousPing.getSpeedKmh() : null,
                previousPing != null ? previousPing.getRecordedAt() : null));
    }
}
