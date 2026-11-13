package com.projectx.grievance.repo;

import com.projectx.grievance.domain.Grievance;
import com.projectx.grievance.domain.GrievanceStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GrievanceRepository extends JpaRepository<Grievance, Long>, JpaSpecificationExecutor<Grievance> {

    /** Callers pass a capped Pageable - a user's own history is bounded in practice, the cap is abuse protection. */
    List<Grievance> findByRaisedByUserIdOrderByCreatedAtDesc(Long raisedByUserId, Pageable pageable);

    List<Grievance> findBySchoolIdOrderByCreatedAtDesc(Long schoolId, Pageable pageable);

    List<Grievance> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatusIn(List<GrievanceStatus> statuses);

    long countBySchoolIdAndStatusIn(Long schoolId, List<GrievanceStatus> statuses);
}
