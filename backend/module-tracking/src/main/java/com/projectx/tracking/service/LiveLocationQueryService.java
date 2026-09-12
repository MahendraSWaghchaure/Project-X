package com.projectx.tracking.service;

import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.school.domain.Route;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.RouteRepository;
import com.projectx.school.repo.VehicleRepository;
import com.projectx.tracking.cache.CachedLocation;
import com.projectx.tracking.cache.LocationCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LiveLocationQueryService {

    private final LocationCacheService locationCacheService;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final GuardianAccessService guardianAccessService;

    public LiveLocationQueryService(LocationCacheService locationCacheService, VehicleRepository vehicleRepository,
                                     RouteRepository routeRepository, ChildRepository childRepository,
                                     ParentProfileRepository parentProfileRepository,
                                     GuardianAccessService guardianAccessService) {
        this.locationCacheService = locationCacheService;
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.guardianAccessService = guardianAccessService;
    }

    public CachedLocation getVehicleLocation(Long schoolId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", vehicleId));
        if (!vehicle.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Vehicle", vehicleId);
        }
        CurrentUserContext.requireSchoolAccess(schoolId);
        return locationCacheService.get(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("No live location yet for vehicle " + vehicleId));
    }

    public CachedLocation getMyChildVehicleLocation(Long childId) {
        Long userId = CurrentUserContext.get().userId();
        ParentProfile parent = parentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for current user"));
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!guardianAccessService.isGuardian(child, parent.getId())) {
            throw new ForbiddenOperationException("Not a guardian of this child");
        }
        if (child.getRouteId() == null) {
            throw new ResourceNotFoundException("Child is not assigned to a route yet");
        }
        Route route = routeRepository.findById(child.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Child's route no longer exists"));
        if (route.getVehicleId() == null) {
            throw new ResourceNotFoundException("Child's route has no vehicle assigned yet");
        }

        return locationCacheService.get(route.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("No live location yet"));
    }
}
