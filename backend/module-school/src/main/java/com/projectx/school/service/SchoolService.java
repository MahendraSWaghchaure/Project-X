package com.projectx.school.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.events.SchoolLocationChangedEvent;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.geo.GeoPoints;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.service.UserProvisioningService;
import com.projectx.school.domain.School;
import com.projectx.school.domain.SchoolAdminProfile;
import com.projectx.school.domain.SchoolStatus;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.SchoolResponse;
import com.projectx.school.dto.UpdateSchoolRequest;
import com.projectx.school.repo.SchoolAdminProfileRepository;
import com.projectx.school.repo.SchoolRepository;
import com.projectx.common.web.PageResponse;
import com.projectx.school.dto.SchoolStatusCounts;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolAdminProfileRepository schoolAdminProfileRepository;
    private final UserProvisioningService userProvisioningService;
    private final ApplicationEventPublisher eventPublisher;

    public SchoolService(SchoolRepository schoolRepository, SchoolAdminProfileRepository schoolAdminProfileRepository,
                          UserProvisioningService userProvisioningService, ApplicationEventPublisher eventPublisher) {
        this.schoolRepository = schoolRepository;
        this.schoolAdminProfileRepository = schoolAdminProfileRepository;
        this.userProvisioningService = userProvisioningService;
        this.eventPublisher = eventPublisher;
    }

    /** Platform admin onboarding flow: create the tenant and its first school admin login atomically. */
    public SchoolResponse createSchool(CreateSchoolRequest request) {
        Point location = (request.latitude() != null && request.longitude() != null)
                ? GeoPoints.toPoint(request.latitude(), request.longitude())
                : null;

        School school = new School(request.name(), request.address(), request.city(), request.state(),
                request.pincode(), request.contactPhone(), request.contactEmail(), location);
        if (request.geofenceRadiusMeters() != null) {
            school.setGeofenceRadiusMeters(request.geofenceRadiusMeters());
        }
        school.setDismissalTime(request.dismissalTime());
        school.setDistrictId(request.districtId());
        school = schoolRepository.save(school);

        UserAccount admin = userProvisioningService.provision(
                request.adminEmail(), null, request.adminPassword(), Role.SCHOOL_ADMIN, school.getId());
        schoolAdminProfileRepository.save(
                new SchoolAdminProfile(admin.getId(), school.getId(), request.adminFirstName(), request.adminLastName()));

        return SchoolResponse.from(school);
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> listSchools(List<Long> districtIds) {
        List<School> schools = (districtIds != null && !districtIds.isEmpty())
                ? schoolRepository.findByDistrictIdIn(districtIds)
                : schoolRepository.findAll();
        return schools.stream().map(SchoolResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SchoolResponse getSchool(Long schoolId) {
        School school = findByIdOrThrow(schoolId);
        CurrentUserContext.requireSchoolAccess(schoolId);
        return SchoolResponse.from(school);
    }

    /** Either the platform admin or that school's own admin can edit its details. */
    public SchoolResponse updateSchool(Long schoolId, UpdateSchoolRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        School school = findByIdOrThrow(schoolId);
        school.updateDetails(request.name(), request.address(), request.city(), request.state(),
                request.pincode(), request.contactPhone(), request.contactEmail());
        if (request.latitude() != null && request.longitude() != null) {
            school.setLocation(GeoPoints.toPoint(request.latitude(), request.longitude()));
        }
        if (request.geofenceRadiusMeters() != null) {
            school.setGeofenceRadiusMeters(request.geofenceRadiusMeters());
        }
        school.setDismissalTime(request.dismissalTime());
        school.setDistrictId(request.districtId());
        eventPublisher.publishEvent(new SchoolLocationChangedEvent(schoolId));
        return SchoolResponse.from(school);
    }

    /**
     * Platform-admin-only: a school can't suspend itself. This is the "delete" a platform admin performs - reversible,
     * data stays intact. Suspending requires a reason (shown on the schools list); re-activating clears it.
     * The audit event is published inside the transaction (AFTER_COMMIT listener) so a rolled-back change leaves no trace.
     */
    public SchoolResponse setSchoolStatus(Long schoolId, SchoolStatus status, String reason) {
        String cleanReason = reason == null || reason.isBlank() ? null : reason.trim();
        if (status == SchoolStatus.SUSPENDED && cleanReason == null) {
            throw new IllegalArgumentException("A reason is required when suspending a school");
        }
        School school = findByIdOrThrow(schoolId);
        SchoolStatus before = school.getStatus();
        school.changeStatus(status, cleanReason);
        eventPublisher.publishEvent(AuditEvent.of("SCHOOL_STATUS_CHANGED", "SCHOOL", schoolId, schoolId,
                before + " -> " + status + (cleanReason == null ? "" : ": " + cleanReason)));
        return SchoolResponse.from(school);
    }

    /** Platform-admin list with the All | Active | Suspended tabs (status), a name/city search (q) and optional district filter; size capped at 100. */
    @Transactional(readOnly = true)
    public PageResponse<SchoolResponse> searchSchools(SchoolStatus status, String q, List<Long> districtIds, Integer page, Integer size) {
        Specification<School> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (districtIds != null && !districtIds.isEmpty()) {
                predicates.add(root.get("districtId").in(districtIds));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("city")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        int pageSize = Math.min(Math.max(size == null ? 20 : size, 1), 100);
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), pageSize, Sort.by("name", "id"));
        return PageResponse.from(schoolRepository.findAll(spec, pageable).map(SchoolResponse::from));
    }

    @Transactional(readOnly = true)
    public SchoolStatusCounts statusCounts() {
        long active = schoolRepository.countByStatus(SchoolStatus.ACTIVE);
        long suspended = schoolRepository.countByStatus(SchoolStatus.SUSPENDED);
        return new SchoolStatusCounts(active + suspended, active, suspended);
    }


    School findByIdOrThrow(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("School", schoolId));
    }
}
