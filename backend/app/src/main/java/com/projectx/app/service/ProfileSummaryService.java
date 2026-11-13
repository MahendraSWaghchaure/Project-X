package com.projectx.app.service;

import com.projectx.app.dto.ProfileSummaryResponse;
import com.projectx.app.dto.ProfileSummaryResponse.ChildSummary;
import com.projectx.app.dto.ProfileSummaryResponse.DriverSummary;
import com.projectx.attendance.domain.TeacherProfile;
import com.projectx.attendance.repo.TeacherProfileRepository;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.school.domain.School;
import com.projectx.school.domain.SchoolAdminProfile;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.SchoolAdminProfileRepository;
import com.projectx.school.repo.SchoolRepository;
import com.projectx.school.repo.VehicleRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backs the grievance "complainant detail" page. The platform admin may look up anyone; a school admin only people who
 * belong to their own school - and for a parent only the children who attend that school (never their other schools).
 * Anything out of scope is a 404, not a 403, so ids cannot be probed.
 */
@Service
@Transactional(readOnly = true)
public class ProfileSummaryService {

    private final UserAccountRepository userAccountRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final SchoolAdminProfileRepository schoolAdminProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final ChildRepository childRepository;
    private final SchoolRepository schoolRepository;
    private final VehicleRepository vehicleRepository;

    public ProfileSummaryService(UserAccountRepository userAccountRepository, ParentProfileRepository parentProfileRepository,
                                 DriverProfileRepository driverProfileRepository,
                                 SchoolAdminProfileRepository schoolAdminProfileRepository,
                                 TeacherProfileRepository teacherProfileRepository, ChildRepository childRepository,
                                 SchoolRepository schoolRepository, VehicleRepository vehicleRepository) {
        this.userAccountRepository = userAccountRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.schoolAdminProfileRepository = schoolAdminProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.childRepository = childRepository;
        this.schoolRepository = schoolRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /** {@code scopeSchoolId} null = platform-wide lookup (platform admin only); otherwise restricted to that school. */
    public ProfileSummaryResponse summary(Long scopeSchoolId, Long userId) {
        CurrentUser caller = CurrentUserContext.get();
        if (scopeSchoolId == null && !caller.isPlatformAdmin()) {
            throw ResourceNotFoundException.of("User", userId);
        }
        if (scopeSchoolId != null) {
            CurrentUserContext.requireSchoolAccess(scopeSchoolId);
        }
        UserAccount user = userAccountRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        String name = user.getEmail();
        String phone = user.getPhone();
        Long schoolId = user.getSchoolId();
        List<Child> children = null;
        DriverProfile driver = null;

        switch (user.getRole()) {
            case PARENT -> {
                ParentProfile p = parentProfileRepository.findByUserId(userId).orElse(null);
                if (p != null) {
                    name = displayName(p.getFirstName(), p.getLastName(), name);
                    phone = p.getPhone() != null ? p.getPhone() : phone;
                    if (schoolId == null) {
                        schoolId = p.getInvitedBySchoolId();
                    }
                    children = childRepository.findByGuardianParentId(p.getId());
                }
            }
            case DRIVER -> {
                driver = driverProfileRepository.findByUserId(userId).orElse(null);
                if (driver != null) {
                    name = displayName(driver.getFirstName(), driver.getLastName(), name);
                    phone = driver.getPhone();
                }
            }
            case SCHOOL_ADMIN -> {
                SchoolAdminProfile a = schoolAdminProfileRepository.findByUserId(userId).orElse(null);
                if (a != null) {
                    name = displayName(a.getFirstName(), a.getLastName(), name);
                }
            }
            case TEACHER -> {
                TeacherProfile t = teacherProfileRepository.findByUserId(userId).orElse(null);
                if (t != null) {
                    name = t.fullName();
                    phone = t.getPhone() != null ? t.getPhone() : phone;
                }
            }
            default -> { }
        }

        if (scopeSchoolId != null) {
            boolean inScope = Objects.equals(schoolId, scopeSchoolId)
                    || (children != null && children.stream().anyMatch(c -> Objects.equals(c.getSchoolId(), scopeSchoolId)));
            if (!inScope) {
                throw ResourceNotFoundException.of("User", userId);
            }
            if (children != null) {
                children = children.stream().filter(c -> Objects.equals(c.getSchoolId(), scopeSchoolId)).toList();
            }
        }

        List<Long> schoolIds = new java.util.ArrayList<>();
        if (schoolId != null) {
            schoolIds.add(schoolId);
        }
        if (children != null) {
            children.forEach(c -> schoolIds.add(c.getSchoolId()));
        }
        if (driver != null && driver.getSchoolId() != null) {
            schoolIds.add(driver.getSchoolId());
        }
        Map<Long, String> schoolNames = schoolRepository.findAllById(schoolIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(School::getId, School::getName));

        List<ChildSummary> childSummaries = children == null ? null : children.stream()
                .map(c -> new ChildSummary(c.getId(), (c.getFirstName() + " " + c.getLastName()).trim(),
                        schoolNames.get(c.getSchoolId()), c.getClassSection())).toList();
        DriverSummary driverSummary = null;
        if (driver != null) {
            String vehicleNo = driver.getVehicleId() == null ? null
                    : vehicleRepository.findById(driver.getVehicleId()).map(Vehicle::getRegistrationNumber).orElse(null);
            driverSummary = new DriverSummary(driver.getId(), driver.getLicenseNumber(), vehicleNo,
                    driver.getEmploymentStatus(), driver.getAddress());
        }
        return new ProfileSummaryResponse(userId, user.getRole(), name, phone, user.getEmail(), schoolId,
                schoolId == null ? null : schoolNames.get(schoolId), childSummaries, driverSummary);
    }

    private static String displayName(String first, String last, String fallback) {
        String n = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return n.isEmpty() ? fallback : n;
    }
}
