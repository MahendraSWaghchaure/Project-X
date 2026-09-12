package com.projectx.people.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.web.PageResponse;
import com.projectx.common.geo.GeoPoints;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ChildStatus;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.dto.AssignChildRouteStopRequest;
import com.projectx.people.dto.ChildDriverResponse;
import com.projectx.people.dto.ChildResponse;
import com.projectx.people.dto.CreateChildRequest;
import com.projectx.people.dto.InviteParentRequest;
import com.projectx.people.dto.InviteParentResponse;
import com.projectx.people.dto.UpdateChildRequest;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.school.domain.Route;
import com.projectx.school.domain.RouteStop;
import com.projectx.school.domain.School;
import com.projectx.school.repo.RouteRepository;
import com.projectx.school.repo.RouteStopRepository;
import com.projectx.school.repo.SchoolRepository;
import java.util.Comparator;
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final ParentService parentService;
    private final SchoolRepository schoolRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final ChildQrTokenService childQrTokenService;
    private final GuardianAccessService guardianAccessService;
    private final ApplicationEventPublisher eventPublisher;

    public ChildService(ChildRepository childRepository, ParentProfileRepository parentProfileRepository,
                         ParentService parentService, SchoolRepository schoolRepository,
                         RouteRepository routeRepository, RouteStopRepository routeStopRepository,
                         DriverProfileRepository driverProfileRepository, ChildQrTokenService childQrTokenService,
                         GuardianAccessService guardianAccessService, ApplicationEventPublisher eventPublisher) {
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.parentService = parentService;
        this.schoolRepository = schoolRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.childQrTokenService = childQrTokenService;
        this.guardianAccessService = guardianAccessService;
        this.eventPublisher = eventPublisher;
    }

    /** School-admin-driven enrollment: request.primaryParentId() must already be a parent invited by this school. */
    public ChildResponse createChildAsSchoolAdmin(Long schoolId, CreateChildRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (request.primaryParentId() == null) {
            throw new IllegalArgumentException("primaryParentId is required");
        }
        ParentProfile parent = parentProfileRepository.findById(request.primaryParentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Parent", request.primaryParentId()));
        if (!parent.getInvitedBySchoolId().equals(schoolId)) {
            throw new ForbiddenOperationException("Parent does not belong to this school");
        }

        Child child = new Child(schoolId, parent.getId(), request.firstName(), request.lastName(),
                request.dateOfBirth(), request.classSection());
        child.updateMedicalSafetyInfo(request.bloodType(), request.emergencyContactName(),
                request.emergencyContactPhone(), request.allergies());
        return ChildResponse.from(childRepository.save(child));
    }

    /** Parent self-service: school and guardianship are derived from the caller's own (already-invited) profile. */
    public ChildResponse createChildAsParent(CreateChildRequest request) {
        ParentProfile parent = parentService.findByUserIdOrThrow(CurrentUserContext.get().userId());
        Child child = new Child(parent.getInvitedBySchoolId(), parent.getId(), request.firstName(), request.lastName(),
                request.dateOfBirth(), request.classSection());
        child.updateMedicalSafetyInfo(request.bloodType(), request.emergencyContactName(),
                request.emergencyContactPhone(), request.allergies());
        return ChildResponse.from(childRepository.save(child));
    }

    @Transactional(readOnly = true)
    public PageResponse<ChildResponse> listChildrenForSchool(Long schoolId, Pageable pageable) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return PageResponse.from(childRepository.findBySchoolId(schoolId, pageable).map(ChildResponse::from));
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> listMyChildren() {
        ParentProfile parent = parentService.findByUserIdOrThrow(CurrentUserContext.get().userId());
        return childRepository.findByGuardianParentId(parent.getId()).stream()
                .map(ChildResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> listChildrenForRoute(Long schoolId, Long routeId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return childRepository.findByRouteId(routeId).stream().map(ChildResponse::from).toList();
    }

    /** Which stop a child boards/alights at - this is what the tracking module's geofence checks key off. Manual override of autoAssignRoute's pick. */
    public ChildResponse assignRouteStop(Long schoolId, Long childId, AssignChildRouteStopRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Child child = findWithinSchool(schoolId, childId);
        child.setRouteId(request.routeId());
        child.setRouteStopId(request.routeStopId());
        return ChildResponse.from(child);
    }

    /**
     * Zone-based auto-allocation: picks the route whose [zoneMinRadiusMeters, zoneMaxRadiusMeters)
     * band contains this child's straight-line distance from the school (e.g. route A covers
     * 0-5km, route B covers 5-10km), then the nearest stop on that route to the child's home.
     * Requires the parent to have already set a home location (see CompleteParentProfileRequest).
     */
    public ChildResponse autoAssignRoute(Long schoolId, Long childId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Child child = findWithinSchool(schoolId, childId);

        ParentProfile parent = parentProfileRepository.findById(child.getPrimaryParentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Parent", child.getPrimaryParentId()));
        Point home = parent.getHomeLocation();
        if (home == null) {
            throw new IllegalStateException("This child's parent hasn't set a home location yet");
        }

        School school = schoolRepository.findById(schoolId).orElseThrow(() -> ResourceNotFoundException.of("School", schoolId));
        if (school.getLocation() == null) {
            throw new IllegalStateException("This school doesn't have a location set yet");
        }
        double distanceFromSchool = GeoPoints.distanceMeters(school.getLocation(), home);

        Route route = routeRepository.findBySchoolId(schoolId).stream()
                .filter(Route::isActive)
                .filter(r -> r.coversDistance(distanceFromSchool))
                .min(Comparator.comparingInt(Route::getZoneMinRadiusMeters))
                .orElseThrow(() -> new IllegalStateException(
                        "No route zone covers a home %.0fm from the school - configure route zones first"
                                .formatted(distanceFromSchool)));

        RouteStop nearestStop = routeStopRepository.findByRouteIdOrderBySequenceNumberAsc(route.getId()).stream()
                .min(Comparator.comparingDouble(stop -> GeoPoints.distanceMeters(stop.getLocation(), home)))
                .orElseThrow(() -> new IllegalStateException("Route " + route.getName() + " has no stops configured"));

        child.setRouteId(route.getId());
        child.setRouteStopId(nearestStop.getId());
        return ChildResponse.from(child);
    }

    public ChildResponse updateChildAsSchoolAdmin(Long schoolId, Long childId, UpdateChildRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Child child = findWithinSchool(schoolId, childId);
        child.updateDetails(request.firstName(), request.lastName(), request.dateOfBirth(), request.classSection());
        child.updateMedicalSafetyInfo(request.bloodType(), request.emergencyContactName(),
                request.emergencyContactPhone(), request.allergies());
        eventPublisher.publishEvent(AuditEvent.of("CHILD_MEDICAL_INFO_UPDATED", "CHILD", child.getId(), schoolId,
                "Medical/safety info updated by school admin"));
        return ChildResponse.from(child);
    }

    public ChildResponse updateChildAsParent(Long childId, UpdateChildRequest request) {
        ParentProfile parent = parentService.findByUserIdOrThrow(CurrentUserContext.get().userId());
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!guardianAccessService.isGuardian(child, parent.getId())) {
            throw new ForbiddenOperationException("Not a guardian of this child");
        }
        child.updateDetails(request.firstName(), request.lastName(), request.dateOfBirth(), request.classSection());
        child.updateMedicalSafetyInfo(request.bloodType(), request.emergencyContactName(),
                request.emergencyContactPhone(), request.allergies());
        eventPublisher.publishEvent(AuditEvent.of("CHILD_MEDICAL_INFO_UPDATED", "CHILD", child.getId(), child.getSchoolId(),
                "Medical/safety info updated by parent"));
        return ChildResponse.from(child);
    }

    /** The trust info (name, phone, verification badge) a parent sees for whoever is driving their child's route today. */
    @Transactional(readOnly = true)
    public ChildDriverResponse getMyChildDriver(Long childId) {
        ParentProfile parent = parentService.findByUserIdOrThrow(CurrentUserContext.get().userId());
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!guardianAccessService.isGuardian(child, parent.getId())) {
            throw new ForbiddenOperationException("Not a guardian of this child");
        }
        if (child.getRouteId() == null) {
            throw new ResourceNotFoundException("This child has not been assigned a route yet");
        }
        Route route = routeRepository.findById(child.getRouteId())
                .orElseThrow(() -> ResourceNotFoundException.of("Route", child.getRouteId()));
        if (route.getVehicleId() == null) {
            throw new ResourceNotFoundException("This child's route has no vehicle assigned yet");
        }
        DriverProfile driver = driverProfileRepository.findByVehicleId(route.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("No driver assigned to this child's vehicle yet"));
        return ChildDriverResponse.from(driver);
    }

    /** The stable QR token a parent can display for the driver to scan instead of a manual boarding tap. */
    @Transactional(readOnly = true)
    public String getMyChildQrToken(Long childId) {
        ParentProfile parent = parentService.findByUserIdOrThrow(CurrentUserContext.get().userId());
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!guardianAccessService.isGuardian(child, parent.getId())) {
            throw new ForbiddenOperationException("Not a guardian of this child");
        }
        return childQrTokenService.generateToken(child.getId());
    }

    @Transactional(readOnly = true)
    public String getChildQrTokenForSchool(Long schoolId, Long childId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Child child = findWithinSchool(schoolId, childId);
        return childQrTokenService.generateToken(child.getId());
    }

    /** Any existing guardian can invite another - unlimited, unlike the old single-secondary-slot model. Reuses the exact same invite/accept machinery as the first parent invite, just recorded as a child_guardians row instead of creating a new child. */
    public InviteParentResponse inviteGuardian(Long childId, InviteParentRequest request) {
        ParentProfile parent = parentService.findByUserIdOrThrow(CurrentUserContext.get().userId());
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!guardianAccessService.isGuardian(child, parent.getId())) {
            throw new ForbiddenOperationException("Not a guardian of this child");
        }
        InviteParentResponse response = parentService.provisionParentInvite(child.getSchoolId(), request);
        guardianAccessService.addGuardian(child.getId(), response.parentProfileId());
        return response;
    }

    /** "Delete" from the school admin's perspective - marking inactive keeps trip/attendance history intact. */
    public void deactivateChild(Long schoolId, Long childId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Child child = findWithinSchool(schoolId, childId);
        child.setStatus(ChildStatus.INACTIVE);
    }

    public ChildResponse linkInsurancePolicy(Long schoolId, Long childId, Long insurancePolicyId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Child child = findWithinSchool(schoolId, childId);
        child.setInsurancePolicyId(insurancePolicyId);
        return ChildResponse.from(child);
    }

    private Child findWithinSchool(Long schoolId, Long childId) {
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!child.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Child", childId);
        }
        return child;
    }
}
