package com.projectx.school.service;

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
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolAdminProfileRepository schoolAdminProfileRepository;
    private final UserProvisioningService userProvisioningService;

    public SchoolService(SchoolRepository schoolRepository, SchoolAdminProfileRepository schoolAdminProfileRepository,
                          UserProvisioningService userProvisioningService) {
        this.schoolRepository = schoolRepository;
        this.schoolAdminProfileRepository = schoolAdminProfileRepository;
        this.userProvisioningService = userProvisioningService;
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
        school = schoolRepository.save(school);

        UserAccount admin = userProvisioningService.provision(
                request.adminEmail(), null, request.adminPassword(), Role.SCHOOL_ADMIN, school.getId());
        schoolAdminProfileRepository.save(
                new SchoolAdminProfile(admin.getId(), school.getId(), request.adminFirstName(), request.adminLastName()));

        return SchoolResponse.from(school);
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> listSchools() {
        return schoolRepository.findAll().stream().map(SchoolResponse::from).toList();
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
        return SchoolResponse.from(school);
    }

    /** Platform-admin-only: a school can't suspend itself. This is the "delete" a platform admin performs - reversible, data stays intact. */
    public SchoolResponse setSchoolStatus(Long schoolId, SchoolStatus status) {
        School school = findByIdOrThrow(schoolId);
        school.setStatus(status);
        return SchoolResponse.from(school);
    }

    School findByIdOrThrow(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("School", schoolId));
    }
}
