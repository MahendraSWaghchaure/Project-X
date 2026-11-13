package com.projectx.school.repo;

import com.projectx.school.domain.Vehicle;
import com.projectx.school.domain.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findBySchoolId(Long schoolId);

    /** Capped listing for the vehicles table. */
    List<Vehicle> findBySchoolId(Long schoolId, Pageable pageable);

    Optional<Vehicle> findByGpsDeviceId(String gpsDeviceId);

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    long countBySchoolId(Long schoolId);

    long countBySchoolIdAndStatus(Long schoolId, VehicleStatus status);
}
