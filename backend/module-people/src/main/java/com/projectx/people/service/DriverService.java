package com.projectx.people.service;

import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.identity.service.UserProvisioningService;
import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.dto.CreateDriverRequest;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.dto.DriverSelfRegisterRequest;
import com.projectx.people.dto.UpdateDriverRequest;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.VehicleRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProvisioningService userProvisioningService;

    public DriverService(DriverProfileRepository driverProfileRepository, VehicleRepository vehicleRepository,
                          UserAccountRepository userAccountRepository, UserProvisioningService userProvisioningService) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.userAccountRepository = userAccountRepository;
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

    /** Public signup: a privately-owned-vehicle driver registers themselves and their vehicle, unaffiliated with any school until claimDriver picks them up. */
    public DriverResponse selfRegister(DriverSelfRegisterRequest request) {
        if (driverProfileRepository.existsByLicenseNumberIgnoreCase(request.licenseNumber())) {
            throw new ConflictException("License already registered: " + request.licenseNumber());
        }
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(request.vehicleRegistrationNumber())) {
            throw new ConflictException("Vehicle already registered: " + request.vehicleRegistrationNumber());
        }

        var user = userProvisioningService.provision(request.email(), request.phone(), request.password(), Role.DRIVER, null);
        Vehicle vehicle = vehicleRepository.save(new Vehicle(null, request.vehicleRegistrationNumber(),
                request.vehicleType(), request.vehicleSeatCapacity(), null));

        DriverProfile driver = new DriverProfile(user.getId(), null, request.firstName(), request.lastName(),
                request.phone(), request.licenseNumber(), request.licenseExpiry(), null);
        driver.setVehicleId(vehicle.getId());
        return DriverResponse.from(driverProfileRepository.save(driver));
    }

    /** The pool of self-registered drivers no school has claimed yet - any school admin can browse it. */
    @Transactional(readOnly = true)
    public List<DriverResponse> listIndependentDrivers() {
        return driverProfileRepository.findBySchoolIdIsNull().stream().map(DriverResponse::from).toList();
    }

    /** A school "hires" an independent driver: their profile, their vehicle, and their login all move to this school. */
    public DriverResponse claimDriver(Long schoolId, Long driverId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> ResourceNotFoundException.of("Driver", driverId));
        if (!driver.isIndependent()) {
            throw new ForbiddenOperationException("This driver already belongs to a school");
        }

        driver.setSchoolId(schoolId);
        if (driver.getVehicleId() != null) {
            vehicleRepository.findById(driver.getVehicleId()).ifPresent(vehicle -> vehicle.setSchoolId(schoolId));
        }
        UserAccount account = userAccountRepository.findById(driver.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No login for this driver"));
        account.setSchoolId(schoolId);

        return DriverResponse.from(driver);
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

    public DriverResponse updateDriver(Long schoolId, Long driverId, UpdateDriverRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = findWithinSchool(schoolId, driverId);
        if (!driver.getLicenseNumber().equalsIgnoreCase(request.licenseNumber())
                && driverProfileRepository.existsByLicenseNumberIgnoreCase(request.licenseNumber())) {
            throw new ConflictException("License already registered: " + request.licenseNumber());
        }
        driver.updateDetails(request.firstName(), request.lastName(), request.phone(), request.licenseNumber(),
                request.licenseExpiry(), request.baseSalaryMonthly());
        return DriverResponse.from(driver);
    }

    /** "Delete" from the UI's perspective - terminating keeps trip/payroll history intact instead of hitting the FK a hard delete would. */
    public void terminateDriver(Long schoolId, Long driverId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = findWithinSchool(schoolId, driverId);
        driver.setEmploymentStatus(DriverEmploymentStatus.TERMINATED);
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
        if (!Objects.equals(driver.getSchoolId(), schoolId)) {
            throw ResourceNotFoundException.of("Driver", driverId);
        }
        return driver;
    }
}
