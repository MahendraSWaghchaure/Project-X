package com.projectx.hardware.repo;

import com.projectx.hardware.domain.RfidTapEvent;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfidTapEventRepository extends JpaRepository<RfidTapEvent, Long> {

    boolean existsByDeviceIdAndEventTimestamp(String deviceId, Instant eventTimestamp);

    long countByDeviceId(String deviceId);
}
