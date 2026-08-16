package com.projectx.tracking.repo;

import com.projectx.tracking.domain.SafetyEventType;
import com.projectx.tracking.domain.VehicleSafetyEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleSafetyEventRepository extends JpaRepository<VehicleSafetyEvent, Long> {

    long countByDriverId(Long driverId);

    List<VehicleSafetyEvent> findByDriverIdOrderByOccurredAtDesc(Long driverId);

    /** Cooldown-based dedup: one flag of a given type per trip per window, so a sustained speeding stretch is one incident, not hundreds. */
    boolean existsByTripIdAndTypeAndOccurredAtAfter(Long tripId, SafetyEventType type, Instant cutoff);
}
