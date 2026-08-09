package com.projectx.payment.repo;

import com.projectx.payment.domain.FeeSchedule;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeScheduleRepository extends JpaRepository<FeeSchedule, Long> {

    Optional<FeeSchedule> findBySchoolId(Long schoolId);
}
