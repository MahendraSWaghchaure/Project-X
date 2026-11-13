package com.projectx.people.repo;

import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.DriverVerificationStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    List<DriverProfile> findBySchoolId(Long schoolId);

    /** Capped listing for the drivers table. */
    List<DriverProfile> findBySchoolId(Long schoolId, Pageable pageable);

    List<DriverProfile> findBySchoolIdIsNull();

    Optional<DriverProfile> findByUserId(Long userId);

    /** Profiles for a bounded set of user ids (display-name lookups) - one query. */
    List<DriverProfile> findByUserIdIn(Collection<Long> userIds);

    Optional<DriverProfile> findByVehicleId(Long vehicleId);

    /** Drivers allocated to any of these vehicles - one query for the whole vehicles page. */
    List<DriverProfile> findByVehicleIdIn(Collection<Long> vehicleIds);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    long countBySchoolId(Long schoolId);

    long countBySchoolIdIsNull();

    long countBySchoolIdAndVerificationStatus(Long schoolId, DriverVerificationStatus verificationStatus);

    /** Drivers whose license is already expired or expires within the reminder window (cutoff = today + window days) - see DriverLicenseExpiryReminderJob. */
    List<DriverProfile> findByLicenseExpiryIsNotNullAndLicenseExpiryLessThanEqual(LocalDate cutoff);
}
