package com.projectx.people.repo;

import com.projectx.people.domain.DriverProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    List<DriverProfile> findBySchoolId(Long schoolId);

    Optional<DriverProfile> findByUserId(Long userId);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);
}
