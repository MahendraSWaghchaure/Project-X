package com.projectx.tracking.service;

import com.projectx.tracking.domain.SafetyEventType;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.domain.VehicleSafetyEvent;
import com.projectx.tracking.repo.VehicleSafetyEventRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs alongside GeofenceEvaluationService on every ingested ping for a trip in progress. This is
 * a threshold rule on reported GPS speed, not real accelerometer telemetry - good enough to
 * surface a directional driver safety score, not a certified safety system.
 */
@Service
@Transactional
@EnableConfigurationProperties(TrackingSafetyProperties.class)
public class SafetyFlagService {

    private final VehicleSafetyEventRepository safetyEventRepository;
    private final TrackingSafetyProperties properties;

    public SafetyFlagService(VehicleSafetyEventRepository safetyEventRepository, TrackingSafetyProperties properties) {
        this.safetyEventRepository = safetyEventRepository;
        this.properties = properties;
    }

    public void evaluate(Trip trip, Double currentSpeedKmh, Instant recordedAt, Double previousSpeedKmh, Instant previousRecordedAt) {
        if (currentSpeedKmh == null) {
            return;
        }
        if (currentSpeedKmh > properties.getSpeedLimitKmh()) {
            raiseOnce(trip, SafetyEventType.OVERSPEED, currentSpeedKmh);
        }
        if (previousSpeedKmh != null && previousRecordedAt != null) {
            Duration gap = Duration.between(previousRecordedAt, recordedAt);
            double drop = previousSpeedKmh - currentSpeedKmh;
            if (!gap.isNegative() && gap.getSeconds() <= properties.getBrakingWindowSeconds()
                    && drop >= properties.getHarshBrakingDeltaKmh()) {
                raiseOnce(trip, SafetyEventType.HARSH_BRAKING, currentSpeedKmh);
            }
        }
    }

    private void raiseOnce(Trip trip, SafetyEventType type, double speedKmh) {
        Instant cutoff = Instant.now().minusSeconds(properties.getCooldownSeconds());
        if (safetyEventRepository.existsByTripIdAndTypeAndOccurredAtAfter(trip.getId(), type, cutoff)) {
            return;
        }
        safetyEventRepository.save(
                new VehicleSafetyEvent(trip.getId(), trip.getVehicleId(), trip.getSchoolId(), trip.getDriverId(), type, speedKmh));
    }
}
