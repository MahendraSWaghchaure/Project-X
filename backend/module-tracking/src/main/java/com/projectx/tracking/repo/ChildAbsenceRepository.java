package com.projectx.tracking.repo;

import com.projectx.tracking.domain.ChildAbsence;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildAbsenceRepository extends JpaRepository<ChildAbsence, Long> {

    Optional<ChildAbsence> findByChildIdAndAbsenceDate(Long childId, LocalDate absenceDate);

    boolean existsByChildIdAndAbsenceDate(Long childId, LocalDate absenceDate);
}
