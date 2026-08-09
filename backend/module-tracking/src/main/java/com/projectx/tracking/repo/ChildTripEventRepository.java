package com.projectx.tracking.repo;

import com.projectx.common.events.ChildTripEventType;
import com.projectx.tracking.domain.ChildTripEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildTripEventRepository extends JpaRepository<ChildTripEvent, Long> {

    List<ChildTripEvent> findByTripId(Long tripId);

    List<ChildTripEvent> findByChildId(Long childId);

    boolean existsByTripIdAndChildIdAndEventType(Long tripId, Long childId, ChildTripEventType eventType);
}
