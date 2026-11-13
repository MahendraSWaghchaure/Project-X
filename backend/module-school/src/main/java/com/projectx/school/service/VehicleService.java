package com.projectx.school.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.school.domain.OwnershipType;
import com.projectx.school.domain.Route;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.domain.VehicleStatus;
import com.projectx.school.dto.CreateVehicleRequest;
import com.projectx.school.dto.UpdateVehicleRequest;
import com.projectx.school.dto.VehicleResponse;
import com.projectx.school.repo.RouteRepository;
import com.projectx.school.repo.VehicleRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleService {

    /** A school has tens of vehicles; the cap only bounds abuse. */
    static final int MAX_VEHICLES_LISTED = 500;

    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final ObjectProvider<VehicleDriverGateway> driverGateway;
    private final ApplicationEventPublisher eventPublisher;

    public VehicleService(VehicleRepository vehicleRepository, RouteRepository routeRepository,
                          ObjectProvider<VehicleDriverGateway> driverGateway, ApplicationEventPublisher eventPublisher) {
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.driverGateway = driverGateway;
        this.eventPublisher = eventPublisher;
    }

    public VehicleResponse createVehicle(Long schoolId, CreateVehicleRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber())) {
            throw new ConflictException("Vehicle already registered: " + request.registrationNumber());
        }
        requireOwnerNameIfPrivate(request.ownershipType(), request.ownerName());
        Vehicle vehicle = new Vehicle(schoolId, request.registrationNumber(), request.type(),
                request.seatCapacity(), request.gpsDeviceId());
        vehicle.setOwner(request.ownershipType(), blankToNull(request.ownerName()), blankToNull(request.ownerPhone()),
                blankToNull(request.ownerAddress()));
        vehicle = vehicleRepository.saveAndFlush(vehicle);
        if (request.assignedDriverId() != null) {
            assignDriverInternal(schoolId, vehicle.getId(), request.assignedDriverId());
        }
        eventPublisher.publishEvent(AuditEvent.of("VEHICLE_CREATED", "VEHICLE", vehicle.getId(), schoolId,
                vehicle.getRegistrationNumber() + " (" + vehicle.getOwnershipType() + ")"));
        return enrichOne(schoolId, vehicle);
    }

    /** Capped at 500; driver name, route name and student count are batched (3 queries total, not per vehicle). */
    @Transactional(readOnly = true)
    public List<VehicleResponse> listVehicles(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        List<Vehicle> vehicles = vehicleRepository.findBySchoolId(schoolId,
                PageRequest.of(0, MAX_VEHICLES_LISTED, Sort.by("registrationNumber", "id")));
        return enrich(schoolId, vehicles);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long schoolId, Long vehicleId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return enrichOne(schoolId, findWithinSchool(schoolId, vehicleId));
    }

    public VehicleResponse updateStatus(Long schoolId, Long vehicleId, VehicleStatus status) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Vehicle vehicle = findWithinSchool(schoolId, vehicleId);
        vehicle.setStatus(status);
        return enrichOne(schoolId, vehicle);
    }

    public VehicleResponse updateVehicle(Long schoolId, Long vehicleId, UpdateVehicleRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Vehicle vehicle = findWithinSchool(schoolId, vehicleId);
        if (!vehicle.getRegistrationNumber().equalsIgnoreCase(request.registrationNumber())
                && vehicleRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber())) {
            throw new ConflictException("Vehicle already registered: " + request.registrationNumber());
        }
        requireOwnerNameIfPrivate(request.ownershipType() != null ? request.ownershipType() : vehicle.getOwnershipType(),
                request.ownerName());
        vehicle.updateDetails(request.registrationNumber(), request.type(), request.seatCapacity());
        vehicle.setGpsDeviceId(request.gpsDeviceId());
        vehicle.setOwner(request.ownershipType(), blankToNull(request.ownerName()), blankToNull(request.ownerPhone()),
                blankToNull(request.ownerAddress()));
        if (request.assignedDriverId() != null) {
            assignDriverInternal(schoolId, vehicleId, request.assignedDriverId());
        }
        return enrichOne(schoolId, vehicle);
    }

    /** Explicit (re)allocation of the vehicle's driver; {@code driverId} null unassigns. */
    public VehicleResponse assignDriver(Long schoolId, Long vehicleId, Long driverId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Vehicle vehicle = findWithinSchool(schoolId, vehicleId);
        assignDriverInternal(schoolId, vehicleId, driverId);
        eventPublisher.publishEvent(AuditEvent.of("VEHICLE_DRIVER_ASSIGNED", "VEHICLE", vehicleId, schoolId,
                driverId == null ? "driver unassigned" : "driverId=" + driverId));
        return enrichOne(schoolId, vehicle);
    }

    /** "Delete" from the UI's perspective - retiring keeps trip history intact instead of hitting the FK a hard delete would. */
    public void retireVehicle(Long schoolId, Long vehicleId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Vehicle vehicle = findWithinSchool(schoolId, vehicleId);
        vehicle.setStatus(VehicleStatus.RETIRED);
    }

    public VehicleResponse linkInsurancePolicy(Long schoolId, Long vehicleId, Long insurancePolicyId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Vehicle vehicle = findWithinSchool(schoolId, vehicleId);
        vehicle.setInsurancePolicyId(insurancePolicyId);
        return enrichOne(schoolId, vehicle);
    }

    // ------------------------------------------------------------------ helpers

    private void assignDriverInternal(Long schoolId, Long vehicleId, Long driverId) {
        VehicleDriverGateway gateway = driverGateway.getIfAvailable();
        if (gateway == null) {
            throw new IllegalStateException("Driver allocation is not available");
        }
        gateway.assignDriver(schoolId, vehicleId, driverId);
    }

    private VehicleResponse enrichOne(Long schoolId, Vehicle vehicle) {
        return enrich(schoolId, List.of(vehicle)).get(0);
    }

    private List<VehicleResponse> enrich(Long schoolId, List<Vehicle> vehicles) {
        if (vehicles.isEmpty()) {
            return List.of();
        }
        List<Long> ids = vehicles.stream().map(Vehicle::getId).toList();
        VehicleDriverGateway gateway = driverGateway.getIfAvailable();
        Map<Long, VehicleDriverGateway.VehicleExtras> extras = gateway == null ? Map.of() : gateway.extrasFor(schoolId, ids);
        Map<Long, String> routeNames = routeNamesByVehicle(ids);
        return vehicles.stream().map(v -> {
            VehicleDriverGateway.VehicleExtras e = extras.getOrDefault(v.getId(), VehicleDriverGateway.VehicleExtras.NONE);
            return VehicleResponse.from(v, e.driverId(), e.driverName(), routeNames.get(v.getId()), e.studentCount());
        }).toList();
    }

    private Map<Long, String> routeNamesByVehicle(Collection<Long> vehicleIds) {
        return routeRepository.findByVehicleIdIn(vehicleIds).stream()
                .collect(Collectors.groupingBy(Route::getVehicleId,
                        Collectors.collectingAndThen(Collectors.toList(),
                                routes -> routes.stream().map(Route::getName).sorted().collect(Collectors.joining(", ")))));
    }

    private static void requireOwnerNameIfPrivate(OwnershipType type, String ownerName) {
        if (type == OwnershipType.PRIVATE && (ownerName == null || ownerName.isBlank())) {
            throw new IllegalArgumentException("ownerName is required for a privately owned vehicle");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private Vehicle findWithinSchool(Long schoolId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleId));
        if (!java.util.Objects.equals(vehicle.getSchoolId(), schoolId)) {
            throw ResourceNotFoundException.of("Vehicle", vehicleId);
        }
        return vehicle;
    }
}
