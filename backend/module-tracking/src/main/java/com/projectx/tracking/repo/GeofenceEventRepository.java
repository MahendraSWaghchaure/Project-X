package com.projectx.tracking.repo;

import com.projectx.common.events.GeofenceEventType;
import com.projectx.tracking.domain.GeofenceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeofenceEventRepository extends JpaRepository<GeofenceEvent, Long> {

    /** School-wide events (APPROACHING_SCHOOL/ARRIVED_SCHOOL/END_OF_DAY_DELAY) - fire once per trip. */
    boolean existsByTripIdAndTypeAndChildIdIsNull(Long tripId, GeofenceEventType type);

    /** Per-stop events (APPROACHING_STOP/ARRIVED_STOP) - fire once per child per trip. */
    boolean existsByTripIdAndTypeAndChildId(Long tripId, GeofenceEventType type, Long childId);
}
