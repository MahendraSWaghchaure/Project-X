package com.projectx.people.service;

import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.dto.AssignChildRouteStopRequest;
import com.projectx.people.dto.ChildResponse;
import com.projectx.people.dto.CreateChildRequest;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final ParentService parentService;

    public ChildService(ChildRepository childRepository, ParentProfileRepository parentProfileRepository,
                         ParentService parentService) {
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.parentService = parentService;
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
        return ChildResponse.from(childRepository.save(child));
    }

    /** Parent self-service: school and guardianship are derived from the caller's own (already-invited) profile. */
    public ChildResponse createChildAsParent(CreateChildRequest request) {
        ParentProfile parent = parentService.findByUserIdOrThrow(CurrentUserContext.get().userId());
        Child child = new Child(parent.getInvitedBySchoolId(), parent.getId(), request.firstName(), request.lastName(),
                request.dateOfBirth(), request.classSection());
        return ChildResponse.from(childRepository.save(child));
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> listChildrenForSchool(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return childRepository.findBySchoolId(schoolId).stream().map(ChildResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> listMyChildren() {
        ParentProfile parent = parentService.findByUserIdOrThrow(CurrentUserContext.get().userId());
        return childRepository.findByPrimaryParentIdOrSecondaryParentId(parent.getId(), parent.getId()).stream()
                .map(ChildResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> listChildrenForRoute(Long schoolId, Long routeId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return childRepository.findByRouteId(routeId).stream().map(ChildResponse::from).toList();
    }

    /** Which stop a child boards/alights at - this is what the tracking module's geofence checks key off. */
    public ChildResponse assignRouteStop(Long schoolId, Long childId, AssignChildRouteStopRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!child.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Child", childId);
        }
        child.setRouteId(request.routeId());
        child.setRouteStopId(request.routeStopId());
        return ChildResponse.from(child);
    }

    public ChildResponse linkInsurancePolicy(Long schoolId, Long childId, Long insurancePolicyId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!child.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Child", childId);
        }
        child.setInsurancePolicyId(insurancePolicyId);
        return ChildResponse.from(child);
    }
}
