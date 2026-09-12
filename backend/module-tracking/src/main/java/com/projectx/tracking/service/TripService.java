package com.projectx.tracking.service;

import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.school.domain.Route;
import com.projectx.school.repo.RouteRepository;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.domain.TripDirection;
import com.projectx.tracking.domain.TripStatus;
import com.projectx.tracking.dto.CreateTripRequest;
import com.projectx.tracking.dto.TripResponse;
import com.projectx.tracking.repo.TripRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TripService {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final GuardianAccessService guardianAccessService;

    public TripService(TripRepository tripRepository, RouteRepository routeRepository,
                        DriverProfileRepository driverProfileRepository, ChildRepository childRepository,
                        ParentProfileRepository parentProfileRepository, GuardianAccessService guardianAccessService) {
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.guardianAccessService = guardianAccessService;
    }

    public TripResponse scheduleTrip(Long schoolId, CreateTripRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Route", request.routeId()));
        if (!route.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Route", request.routeId());
        }
        if (route.getVehicleId() == null || route.getDriverId() == null) {
            throw new IllegalStateException("Route has no vehicle/driver assigned yet");
        }

        Trip trip = new Trip(schoolId, route.getId(), route.getVehicleId(), route.getDriverId(),
                request.direction(), request.tripDate());
        return TripResponse.from(tripRepository.save(trip));
    }

    public TripResponse startTrip(Long tripId) {
        Trip trip = findAndAuthorizeAsDriverOrSchoolAdmin(tripId);
        trip.start();
        return TripResponse.from(trip);
    }

    public TripResponse completeTrip(Long tripId) {
        Trip trip = findAndAuthorizeAsDriverOrSchoolAdmin(tripId);
        trip.complete();
        return TripResponse.from(trip);
    }

    public TripResponse cancelTrip(Long tripId) {
        Trip trip = findAndAuthorizeAsDriverOrSchoolAdmin(tripId);
        trip.cancel();
        return TripResponse.from(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> listTripsForSchool(Long schoolId, LocalDate date) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return tripRepository.findBySchoolIdAndTripDate(schoolId, date).stream().map(TripResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TripResponse> listMyTripsAsDriver(LocalDate date) {
        Long driverId = currentDriverProfileId();
        return tripRepository.findByDriverIdAndTripDate(driverId, date).stream().map(TripResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> ResourceNotFoundException.of("Trip", tripId));
        CurrentUserContext.requireSchoolAccess(trip.getSchoolId());
        return TripResponse.from(trip);
    }

    /** Lets a parent resolve which trip today is theirs to act on (e.g. confirming home arrival), without exposing the school-wide trip listing to them. */
    @Transactional(readOnly = true)
    public TripResponse getTodaysTripForMyChild(Long childId, TripDirection direction) {
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

        return tripRepository.findBySchoolIdAndTripDate(child.getSchoolId(), LocalDate.now()).stream()
                .filter(trip -> trip.getRouteId().equals(child.getRouteId()) && trip.getDirection() == direction)
                .findFirst()
                .map(TripResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No trip scheduled today for this child"));
    }

    Trip findAndAuthorizeAsDriverOrSchoolAdmin(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> ResourceNotFoundException.of("Trip", tripId));
        CurrentUserContext.requireSchoolAccess(trip.getSchoolId());
        var currentUser = CurrentUserContext.get();
        if (currentUser.role() == com.projectx.common.security.Role.DRIVER) {
            Long driverId = currentDriverProfileId();
            if (!trip.getDriverId().equals(driverId)) {
                throw new ForbiddenOperationException("Not the driver assigned to this trip");
            }
        }
        return trip;
    }

    private Long currentDriverProfileId() {
        Long userId = CurrentUserContext.get().userId();
        return driverProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenOperationException("No driver profile for current user"))
                .getId();
    }

    List<Trip> findActiveTrips() {
        return tripRepository.findByStatusIn(List.of(TripStatus.IN_PROGRESS, TripStatus.DELAYED));
    }
}
