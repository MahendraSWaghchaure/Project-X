package com.projectx.grievance.repo;

import com.projectx.grievance.domain.Grievance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    List<Grievance> findByRaisedByUserIdOrderByCreatedAtDesc(Long raisedByUserId);

    List<Grievance> findBySchoolIdOrderByCreatedAtDesc(Long schoolId);

    List<Grievance> findAllByOrderByCreatedAtDesc();
}
