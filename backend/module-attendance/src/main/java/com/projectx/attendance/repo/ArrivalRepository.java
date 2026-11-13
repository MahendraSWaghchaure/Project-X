package com.projectx.attendance.repo;

import com.projectx.attendance.domain.Arrival;
import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.ArrivalOutcome;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArrivalRepository extends JpaRepository<Arrival, Long> {

    Optional<Arrival> findByTripIdAndChildIdAndKind(Long tripId, Long childId, ArrivalKind kind);

    /** Capped by the caller's Pageable - one trip carries at most a vehicle's worth of children. */
    List<Arrival> findByTripId(Long tripId, Pageable pageable);

    /** Backs the bulk-attendance write path: arrivals of the (at most 500) students just marked. */
    List<Arrival> findByStudentIdInAndArrivalDate(Collection<Long> studentIds, LocalDate arrivalDate);

    Page<Arrival> findBySchoolIdAndArrivalDate(Long schoolId, LocalDate arrivalDate, Pageable pageable);

    Page<Arrival> findBySchoolIdAndArrivalDateAndOutcome(Long schoolId, LocalDate arrivalDate, ArrivalOutcome outcome, Pageable pageable);

    @Query("select a from Arrival a where a.childId in :childIds and a.parentResponse = com.projectx.attendance.domain.ParentResponse.PENDING "
            + "and a.arrivalDate >= :since order by a.driverEventAt desc")
    List<Arrival> findPendingForChildren(@Param("childIds") Collection<Long> childIds, @Param("since") LocalDate since, Pageable pageable);
}
