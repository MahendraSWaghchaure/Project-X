package com.projectx.tracking.service;

import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.tracking.dto.SafetyEventResponse;
import com.projectx.tracking.repo.VehicleSafetyEventRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SafetyEventQueryService {

    private final VehicleSafetyEventRepository safetyEventRepository;
    private final DriverProfileRepository driverProfileRepository;

    public SafetyEventQueryService(VehicleSafetyEventRepository safetyEventRepository, DriverProfileRepository driverProfileRepository) {
        this.safetyEventRepository = safetyEventRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    /** The per-driver safety score a school admin sees: raw flagged-incident count plus the incidents themselves, most recent first. */
    public List<SafetyEventResponse> listForDriver(Long schoolId, Long driverId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> ResourceNotFoundException.of("Driver", driverId));
        if (!Objects.equals(driver.getSchoolId(), schoolId)) {
            throw ResourceNotFoundException.of("Driver", driverId);
        }
        return safetyEventRepository.findByDriverIdOrderByOccurredAtDesc(driverId).stream()
                .map(SafetyEventResponse::from)
                .toList();
    }
}
