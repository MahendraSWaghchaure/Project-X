package com.projectx.people.service;

import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.service.UserProvisioningService;
import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.dto.CreateDriverRequest;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.repo.DriverProfileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final UserProvisioningService userProvisioningService;

    public DriverService(DriverProfileRepository driverProfileRepository, UserProvisioningService userProvisioningService) {
        this.driverProfileRepository = driverProfileRepository;
        this.userProvisioningService = userProvisioningService;
    }

    public DriverResponse createDriver(Long schoolId, CreateDriverRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (driverProfileRepository.existsByLicenseNumberIgnoreCase(request.licenseNumber())) {
            throw new ConflictException("License already registered: " + request.licenseNumber());
        }

        var user = userProvisioningService.provision(request.email(), request.phone(), request.password(), Role.DRIVER, schoolId);
        DriverProfile driver = new DriverProfile(user.getId(), schoolId, request.firstName(), request.lastName(),
                request.phone(), request.licenseNumber(), request.licenseExpiry(), request.baseSalaryMonthly());
        return DriverResponse.from(driverProfileRepository.save(driver));
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> listDrivers(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return driverProfileRepository.findBySchoolId(schoolId).stream().map(DriverResponse::from).toList();
    }

    public DriverResponse updateEmploymentStatus(Long schoolId, Long driverId, DriverEmploymentStatus status) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = findWithinSchool(schoolId, driverId);
        driver.setEmploymentStatus(status);
        return DriverResponse.from(driver);
    }

    public DriverResponse linkInsurancePolicy(Long schoolId, Long driverId, Long insurancePolicyId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = findWithinSchool(schoolId, driverId);
        driver.setInsurancePolicyId(insurancePolicyId);
        return DriverResponse.from(driver);
    }

    private DriverProfile findWithinSchool(Long schoolId, Long driverId) {
        DriverProfile driver = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> ResourceNotFoundException.of("Driver", driverId));
        if (!driver.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Driver", driverId);
        }
        return driver;
    }
}
