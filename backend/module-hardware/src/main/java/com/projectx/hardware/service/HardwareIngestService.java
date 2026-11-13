package com.projectx.hardware.service;

import com.projectx.hardware.domain.RfidTapEvent;
import com.projectx.hardware.domain.VehicleTelemetrySample;
import com.projectx.hardware.dto.RfidTapEventRequest;
import com.projectx.hardware.dto.VehicleTelemetrySampleRequest;
import com.projectx.hardware.repo.RfidTapEventRepository;
import com.projectx.hardware.repo.VehicleTelemetrySampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pure data capture for hardware that doesn't exist yet - see HardwareIngestController's javadoc.
 * Idempotent on (deviceId, timestamp): the exists-check below is the fast path, and the unique
 * index from the V110 migration is the actual guarantee under a race (two near-simultaneous
 * retries both passing the exists-check) - the resulting DataIntegrityViolationException is caught
 * and treated as "already recorded", not an error, exactly the same tolerance a flaky device's
 * retry needs.
 */
@Service
@Transactional
public class HardwareIngestService {

    private static final Logger log = LoggerFactory.getLogger(HardwareIngestService.class);

    private final RfidTapEventRepository tapEventRepository;
    private final VehicleTelemetrySampleRepository telemetrySampleRepository;

    public HardwareIngestService(RfidTapEventRepository tapEventRepository, VehicleTelemetrySampleRepository telemetrySampleRepository) {
        this.tapEventRepository = tapEventRepository;
        this.telemetrySampleRepository = telemetrySampleRepository;
    }

    /** Returns true if a new row was stored, false if (deviceId, timestamp) was already recorded. */
    public boolean recordTap(RfidTapEventRequest request) {
        if (tapEventRepository.existsByDeviceIdAndEventTimestamp(request.deviceId(), request.timestamp())) {
            return false;
        }
        try {
            tapEventRepository.save(new RfidTapEvent(request.deviceId(), request.cardToken(), request.gateType(), request.timestamp()));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate RFID tap event (device={}, timestamp={}) - treating retry as already recorded",
                    request.deviceId(), request.timestamp());
            return false;
        }
    }

    /** Returns true if a new row was stored, false if (deviceId, timestamp) was already recorded. */
    public boolean recordTelemetry(VehicleTelemetrySampleRequest request) {
        if (telemetrySampleRepository.existsByDeviceIdAndSampleTimestamp(request.deviceId(), request.timestamp())) {
            return false;
        }
        try {
            telemetrySampleRepository.save(new VehicleTelemetrySample(request.deviceId(), request.speedKmh(),
                    request.harshBraking(), request.doorOpen(), request.timestamp()));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate vehicle telemetry sample (device={}, timestamp={}) - treating retry as already recorded",
                    request.deviceId(), request.timestamp());
            return false;
        }
    }
}
