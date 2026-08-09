package com.projectx.tracking.repo;

import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.domain.TripStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByVehicleIdAndStatusIn(Long vehicleId, List<TripStatus> statuses);

    List<Trip> findBySchoolIdAndTripDate(Long schoolId, LocalDate tripDate);

    List<Trip> findByDriverIdAndTripDate(Long driverId, LocalDate tripDate);

    List<Trip> findByStatusIn(List<TripStatus> statuses);
}
