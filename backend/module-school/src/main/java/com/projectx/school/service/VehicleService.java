package com.projectx.school.service;

import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.domain.VehicleStatus;
import com.projectx.school.dto.CreateVehicleRequest;
import com.projectx.school.dto.VehicleResponse;
import com.projectx.school.repo.VehicleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleResponse createVehicle(Long schoolId, CreateVehicleRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber())) {
            throw new ConflictException("Vehicle already registered: " + request.registrationNumber());
        }
        Vehicle vehicle = new Vehicle(schoolId, request.registrationNumber(), request.type(),
                request.seatCapacity(), request.gpsDeviceId());
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> listVehicles(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return vehicleRepository.findBySchoolId(schoolId).stream().map(VehicleResponse::from).toList();
    }

    public VehicleResponse updateStatus(Long schoolId, Long vehicleId, VehicleStatus status) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Vehicle vehicle = findWithinSchool(schoolId, vehicleId);
        vehicle.setStatus(status);
        return VehicleResponse.from(vehicle);
    }

    public VehicleResponse linkInsurancePolicy(Long schoolId, Long vehicleId, Long insurancePolicyId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Vehicle vehicle = findWithinSchool(schoolId, vehicleId);
        vehicle.setInsurancePolicyId(insurancePolicyId);
        return VehicleResponse.from(vehicle);
    }

    private Vehicle findWithinSchool(Long schoolId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleId));
        if (!vehicle.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Vehicle", vehicleId);
        }
        return vehicle;
    }
}
