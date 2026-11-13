package com.projectx.people.service;

import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.VehicleRepository;
import com.projectx.school.service.VehicleDriverGateway;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implements module-school's seam: driver allocation and per-vehicle student counts live here because drivers and children do. */
@Component
@Transactional
public class PeopleVehicleDriverGateway implements VehicleDriverGateway {

    private final DriverProfileRepository driverProfileRepository;
    private final ChildRepository childRepository;
    private final VehicleRepository vehicleRepository;

    public PeopleVehicleDriverGateway(DriverProfileRepository driverProfileRepository, ChildRepository childRepository,
                                      VehicleRepository vehicleRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.childRepository = childRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, VehicleExtras> extrasFor(Long schoolId, Collection<Long> vehicleIds) {
        Map<Long, VehicleExtras> out = new HashMap<>();
        if (vehicleIds.isEmpty()) {
            return out;
        }
        Map<Long, DriverProfile> driverByVehicle = new HashMap<>();
        for (DriverProfile d : driverProfileRepository.findByVehicleIdIn(vehicleIds)) {
            driverByVehicle.putIfAbsent(d.getVehicleId(), d);
        }
        Map<Long, Long> students = new HashMap<>();
        for (Object[] row : childRepository.countActiveByVehicle(vehicleIds)) {
            students.put((Long) row[0], ((Number) row[1]).longValue());
        }
        for (Long vehicleId : vehicleIds) {
            DriverProfile d = driverByVehicle.get(vehicleId);
            out.put(vehicleId, new VehicleExtras(d == null ? null : d.getId(),
                    d == null ? null : d.getFirstName() + " " + d.getLastName(), students.getOrDefault(vehicleId, 0L)));
        }
        return out;
    }

    @Override
    public void assignDriver(Long schoolId, Long vehicleId, Long driverId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleId));
        if (!Objects.equals(vehicle.getSchoolId(), schoolId)) {
            throw ResourceNotFoundException.of("Vehicle", vehicleId);
        }
        DriverProfile target = null;
        if (driverId != null) {
            target = driverProfileRepository.findById(driverId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Driver", driverId));
            if (!Objects.equals(target.getSchoolId(), schoolId)) {
                // Same answer as a missing driver: never confirm another tenant's driver id exists.
                throw ResourceNotFoundException.of("Driver", driverId);
            }
            if (target.getEmploymentStatus() == DriverEmploymentStatus.TERMINATED) {
                throw new ForbiddenOperationException("A terminated driver cannot be allocated a vehicle");
            }
        }
        // One driver per vehicle, one vehicle per driver: release both existing links first.
        for (DriverProfile holder : driverProfileRepository.findByVehicleIdIn(java.util.List.of(vehicleId))) {
            if (target == null || !holder.getId().equals(target.getId())) {
                holder.setVehicleId(null);
            }
        }
        if (target != null) {
            target.setVehicleId(vehicleId);
        }
        driverProfileRepository.flush();
    }
}
