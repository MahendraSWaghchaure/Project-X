package com.projectx.app.service;

import com.projectx.attendance.domain.TeacherProfile;
import com.projectx.attendance.repo.TeacherProfileRepository;
import com.projectx.common.security.Role;
import com.projectx.grievance.service.GrievanceDirectory;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.school.domain.School;
import com.projectx.school.domain.SchoolAdminProfile;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.SchoolAdminProfileRepository;
import com.projectx.school.repo.SchoolRepository;
import com.projectx.school.repo.VehicleRepository;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.repo.TripRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The composition-root implementation of module-grievance's directory seam: display names for the grievances table (batched,
 * one query per kind) and server-side verification that the optional links on a new grievance are the submitter's to make.
 * Every "not yours" and "does not exist" answer is the same 400, so ids of other tenants cannot be probed.
 */
@Component
@Transactional(readOnly = true)
public class GrievanceDirectoryImpl implements GrievanceDirectory {

    private final UserAccountRepository userAccountRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final SchoolAdminProfileRepository schoolAdminProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final ChildRepository childRepository;
    private final SchoolRepository schoolRepository;
    private final TripRepository tripRepository;
    private final GuardianAccessService guardianAccessService;

    public GrievanceDirectoryImpl(UserAccountRepository userAccountRepository, ParentProfileRepository parentProfileRepository,
                                  DriverProfileRepository driverProfileRepository,
                                  SchoolAdminProfileRepository schoolAdminProfileRepository,
                                  TeacherProfileRepository teacherProfileRepository, VehicleRepository vehicleRepository,
                                  ChildRepository childRepository, SchoolRepository schoolRepository,
                                  TripRepository tripRepository, GuardianAccessService guardianAccessService) {
        this.userAccountRepository = userAccountRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.schoolAdminProfileRepository = schoolAdminProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.childRepository = childRepository;
        this.schoolRepository = schoolRepository;
        this.tripRepository = tripRepository;
        this.guardianAccessService = guardianAccessService;
    }

    @Override
    public Map<Long, String> userNames(Collection<Long> userIds) {
        Map<Long, String> out = new HashMap<>();
        if (userIds.isEmpty()) {
            return out;
        }
        for (ParentProfile p : parentProfileRepository.findByUserIdIn(userIds)) {
            putName(out, p.getUserId(), p.getFirstName(), p.getLastName());
        }
        for (DriverProfile d : driverProfileRepository.findByUserIdIn(userIds)) {
            putName(out, d.getUserId(), d.getFirstName(), d.getLastName());
        }
        for (SchoolAdminProfile a : schoolAdminProfileRepository.findByUserIdIn(userIds)) {
            putName(out, a.getUserId(), a.getFirstName(), a.getLastName());
        }
        for (TeacherProfile t : teacherProfileRepository.findByUserIdIn(userIds)) {
            putName(out, t.getUserId(), t.getFirstName(), t.getLastName());
        }
        for (UserAccount u : userAccountRepository.findAllById(userIds)) {
            out.putIfAbsent(u.getId(), u.getEmail());
        }
        return out;
    }

    @Override
    public Map<Long, String> driverNames(Collection<Long> driverProfileIds) {
        Map<Long, String> out = new HashMap<>();
        if (!driverProfileIds.isEmpty()) {
            for (DriverProfile d : driverProfileRepository.findAllById(driverProfileIds)) {
                out.put(d.getId(), (d.getFirstName() + " " + d.getLastName()).trim());
            }
        }
        return out;
    }

    @Override
    public Map<Long, String> vehicleNumbers(Collection<Long> vehicleIds) {
        Map<Long, String> out = new HashMap<>();
        if (!vehicleIds.isEmpty()) {
            for (Vehicle v : vehicleRepository.findAllById(vehicleIds)) {
                out.put(v.getId(), v.getRegistrationNumber());
            }
        }
        return out;
    }

    @Override
    public Map<Long, String> childNames(Collection<Long> childIds) {
        Map<Long, String> out = new HashMap<>();
        if (!childIds.isEmpty()) {
            for (Child c : childRepository.findAllById(childIds)) {
                out.put(c.getId(), (c.getFirstName() + " " + c.getLastName()).trim());
            }
        }
        return out;
    }

    @Override
    public Map<Long, String> schoolNames(Collection<Long> schoolIds) {
        Map<Long, String> out = new HashMap<>();
        if (!schoolIds.isEmpty()) {
            for (School s : schoolRepository.findAllById(schoolIds)) {
                out.put(s.getId(), s.getName());
            }
        }
        return out;
    }

    @Override
    public void validateLinks(Role role, Long userId, Long schoolId, Long vehicleId, Long driverId, Long childId, Long tripId) {
        DriverProfile ownDriver = role == Role.DRIVER ? driverProfileRepository.findByUserId(userId).orElse(null) : null;

        if (role == Role.PARENT && schoolId != null) {
            // A parent's token carries no school, so the claimed schoolId must be one they genuinely belong to: the school that
            // invited them, or the school of a child they guard. Otherwise they could file into (and probe) any school.
            ParentProfile parent = parentProfileRepository.findByUserId(userId).orElse(null);
            boolean member = parent != null && (Objects.equals(parent.getInvitedBySchoolId(), schoolId)
                    || childRepository.findByGuardianParentId(parent.getId()).stream().anyMatch(c -> Objects.equals(c.getSchoolId(), schoolId)));
            if (!member) {
                throw unknown("school");
            }
        }

        if (childId != null) {
            Child child = childRepository.findById(childId).orElseThrow(() -> unknown("child"));
            boolean mine = switch (role) {
                case PARENT -> parentProfileRepository.findByUserId(userId)
                        .map(p -> guardianAccessService.isGuardian(child, p.getId())).orElse(false);
                default -> schoolId != null && Objects.equals(child.getSchoolId(), schoolId);
            };
            if (!mine || (schoolId != null && !Objects.equals(child.getSchoolId(), schoolId))) {
                throw unknown("child");
            }
        }
        if (vehicleId != null) {
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() -> unknown("vehicle"));
            boolean ok = schoolId != null ? Objects.equals(vehicle.getSchoolId(), schoolId)
                    : ownDriver != null && Objects.equals(ownDriver.getVehicleId(), vehicleId);
            if (!ok) {
                throw unknown("vehicle");
            }
        }
        if (driverId != null) {
            DriverProfile driver = driverProfileRepository.findById(driverId).orElseThrow(() -> unknown("driver"));
            boolean ok = schoolId != null ? Objects.equals(driver.getSchoolId(), schoolId)
                    : ownDriver != null && Objects.equals(ownDriver.getId(), driverId);
            if (!ok) {
                throw unknown("driver");
            }
        }
        if (tripId != null) {
            Trip trip = tripRepository.findById(tripId).orElseThrow(() -> unknown("trip"));
            boolean ok = Objects.equals(trip.getSchoolId(), schoolId)
                    && (ownDriver == null || Objects.equals(trip.getDriverId(), ownDriver.getId()));
            if (!ok) {
                throw unknown("trip");
            }
        }
    }

    private static IllegalArgumentException unknown(String what) {
        return new IllegalArgumentException("Unknown " + what);
    }

    private static void putName(Map<Long, String> out, Long userId, String first, String last) {
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        if (!name.isEmpty()) {
            out.put(userId, name);
        }
    }
}
