package com.projectx.tracking.repo;

import com.projectx.tracking.domain.SosEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SosEventRepository extends JpaRepository<SosEvent, Long> {

    List<SosEvent> findBySchoolIdAndResolvedAtIsNull(Long schoolId);
}
