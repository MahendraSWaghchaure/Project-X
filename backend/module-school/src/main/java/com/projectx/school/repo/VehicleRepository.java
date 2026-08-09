package com.projectx.school.repo;

import com.projectx.school.domain.Vehicle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findBySchoolId(Long schoolId);

    Optional<Vehicle> findByGpsDeviceId(String gpsDeviceId);

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
}
