package com.projectx.people.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.identity.service.UserProvisioningService;
import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.DriverVerificationStatus;
import com.projectx.people.dto.CreateDriverRequest;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.dto.DriverSelfRegisterRequest;
import com.projectx.people.dto.UpdateDriverRequest;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.VehicleRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DriverService {

    /** A school has tens of drivers; the cap only bounds abuse. */
    static final int MAX_DRIVERS_LISTED = 500;

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProvisioningService userProvisioningService;
    private final ApplicationEventPublisher eventPublisher;

    public DriverService(DriverProfileRepository driverProfileRepository, VehicleRepository vehicleRepository,
                          UserAccountRepository userAccountRepository, UserProvisioningService userProvisioningService,
                          ApplicationEventPublisher eventPublisher) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.userAccountRepository = userAccountRepository;
        this.userProvisioningService = userProvisioningService;
        this.eventPublisher = eventPublisher;
    }

    public DriverResponse createDriver(Long schoolId, CreateDriverRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (driverProfileRepository.existsByLicenseNumberIgnoreCase(request.licenseNumber())) {
            throw new ConflictException("License already registered: " + request.licenseNumber());
        }

        var user = userProvisioningService.provision(request.email(), request.phone(), request.password(), Role.DRIVER, schoolId);
        DriverProfile driver = new DriverProfile(user.getId(), schoolId, request.firstName(), request.lastName(),
                request.phone(), request.licenseNumber(), request.licenseExpiry(), request.baseSalaryMonthly());
        driver.setAddress(blankToNull(request.address()));
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

    /** Capped at 500; allocated-vehicle registration numbers are fetched in one batched query. */
    @Transactional(readOnly = true)
    public List<DriverResponse> listDrivers(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        List<DriverProfile> drivers = driverProfileRepository.findBySchoolId(schoolId,
                PageRequest.of(0, MAX_DRIVERS_LISTED, Sort.by("firstName", "lastName", "id")));
        Map<Long, String> vehicleNumbers = vehicleNumbers(drivers);
        return drivers.stream().map(d -> DriverResponse.from(d, vehicleNumbers.get(d.getVehicleId()))).toList();
    }

    /**
     * SUSPENDED and TERMINATED require a reason (stored with the time of change); moving back to ACTIVE/ON_LEAVE clears it.
     * The audit event is published inside the transaction.
     */
    public DriverResponse updateEmploymentStatus(Long schoolId, Long driverId, DriverEmploymentStatus status, String reason) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = findWithinSchool(schoolId, driverId);
        String cleanReason = blankToNull(reason);
        if ((status == DriverEmploymentStatus.SUSPENDED || status == DriverEmploymentStatus.TERMINATED) && cleanReason == null) {
            throw new IllegalArgumentException("A reason is required when setting a driver to " + status);
        }
        DriverEmploymentStatus before = driver.getEmploymentStatus();
        driver.changeEmploymentStatus(status, cleanReason);
        eventPublisher.publishEvent(AuditEvent.of("DRIVER_EMPLOYMENT_STATUS_CHANGED", "DRIVER_PROFILE", driver.getId(), schoolId,
                before + " -> " + status + (cleanReason == null ? "" : ": " + cleanReason)));
        return DriverResponse.from(driver, vehicleNumber(driver));
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
        if (request.address() != null) {
            driver.setAddress(blankToNull(request.address()));
        }
        return DriverResponse.from(driver, vehicleNumber(driver));
    }

    /** "Delete" from the UI's perspective - terminating keeps trip/payroll history intact instead of hitting the FK a hard delete would. */
    public void terminateDriver(Long schoolId, Long driverId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = findWithinSchool(schoolId, driverId);
        driver.changeEmploymentStatus(DriverEmploymentStatus.TERMINATED, "Terminated by school");
        eventPublisher.publishEvent(AuditEvent.of("DRIVER_TERMINATED", "DRIVER_PROFILE", driver.getId(), schoolId,
                driver.getFirstName() + " " + driver.getLastName() + " terminated"));
    }

    /** School admin sets a driver's background-verification status - the badge a parent sees before trusting them with their child. */
    public DriverResponse updateVerificationStatus(Long schoolId, Long driverId, DriverVerificationStatus status) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = findWithinSchool(schoolId, driverId);
        CurrentUser actor = CurrentUserContext.get();
        driver.updateVerification(status, actor.userId());
        eventPublisher.publishEvent(AuditEvent.of("DRIVER_VERIFICATION_CHANGED", "DRIVER_PROFILE", driver.getId(), schoolId,
                "Verification status set to " + status));
        return DriverResponse.from(driver);
    }

    public DriverResponse linkInsurancePolicy(Long schoolId, Long driverId, Long insurancePolicyId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        DriverProfile driver = findWithinSchool(schoolId, driverId);
        driver.setInsurancePolicyId(insurancePolicyId);
        return DriverResponse.from(driver);
    }

    private String vehicleNumber(DriverProfile driver) {
        return driver.getVehicleId() == null ? null
                : vehicleRepository.findById(driver.getVehicleId()).map(Vehicle::getRegistrationNumber).orElse(null);
    }

    private Map<Long, String> vehicleNumbers(List<DriverProfile> drivers) {
        List<Long> ids = drivers.stream().map(DriverProfile::getVehicleId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            // Map.of() throws NPE on get(null) - most drivers in this list have no vehicle
            // assigned (getVehicleId() == null), and the caller looks every driver up by id.
            return new HashMap<>();
        }
        return vehicleRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getRegistrationNumber));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
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
