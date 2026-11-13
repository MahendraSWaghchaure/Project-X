package com.projectx.grievance.repo;

import com.projectx.grievance.domain.GrievanceEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrievanceEventRepository extends JpaRepository<GrievanceEvent, Long> {

    /** A grievance has a handful of status changes; bounded by its own lifecycle. */
    List<GrievanceEvent> findByGrievanceIdOrderByCreatedAtAscIdAsc(Long grievanceId);
}
