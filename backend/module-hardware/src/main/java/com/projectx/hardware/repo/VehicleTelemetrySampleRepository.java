package com.projectx.hardware.repo;

import com.projectx.hardware.domain.VehicleTelemetrySample;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTelemetrySampleRepository extends JpaRepository<VehicleTelemetrySample, Long> {

    boolean existsByDeviceIdAndSampleTimestamp(String deviceId, Instant sampleTimestamp);

    long countByDeviceId(String deviceId);
}
