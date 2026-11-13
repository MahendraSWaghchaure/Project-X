package com.projectx.app.service;

import com.projectx.app.dto.SchoolOverviewResponse;
import com.projectx.app.dto.SchoolOverviewResponse.DriverRow;
import com.projectx.attendance.repo.StudentRepository;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.grievance.domain.GrievanceStatus;
import com.projectx.grievance.repo.GrievanceRepository;
import com.projectx.grievance.service.GrievanceService;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.dto.SchoolResponse;
import com.projectx.school.repo.VehicleRepository;
import com.projectx.school.service.SchoolService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * B1: the platform admin's "click a school -> detail page" endpoint. Lives in {@code app} (not
 * module-school) because it composes school + people (drivers) + school (vehicles) + attendance
 * (roster) + grievance - the same cross-module-composition-root rule as DashboardStatsService.
 */
@Service
@Transactional(readOnly = true)
public class SchoolOverviewService {

    /** A school's driver roster is at most a few hundred - same cap as the drivers table (Phase 2 convention). */
    private static final int MAX_DRIVERS = 500;
    private static final int RECENT_GRIEVANCES = 5;
    private static final List<GrievanceStatus> OPEN_GRIEVANCE_STATUSES = List.of(GrievanceStatus.OPEN, GrievanceStatus.IN_PROGRESS);

    private final SchoolService schoolService;
    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final StudentRepository studentRepository;
    private final GrievanceRepository grievanceRepository;
    private final GrievanceService grievanceService;

    public SchoolOverviewService(SchoolService schoolService, DriverProfileRepository driverProfileRepository,
                                 VehicleRepository vehicleRepository, StudentRepository studentRepository,
                                 GrievanceRepository grievanceRepository, GrievanceService grievanceService) {
        this.schoolService = schoolService;
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.studentRepository = studentRepository;
        this.grievanceRepository = grievanceRepository;
        this.grievanceService = grievanceService;
    }

    public SchoolOverviewResponse overview(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        SchoolResponse school = schoolService.getSchool(schoolId);

        List<DriverProfile> driverProfiles = driverProfileRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_DRIVERS));
        Map<Long, String> vehicleNumbers = new HashMap<>();
        List<Long> vehicleIds = driverProfiles.stream().map(DriverProfile::getVehicleId).filter(java.util.Objects::nonNull).toList();
        if (!vehicleIds.isEmpty()) {
            for (Vehicle v : vehicleRepository.findAllById(vehicleIds)) {
                vehicleNumbers.put(v.getId(), v.getRegistrationNumber());
            }
        }
        List<DriverRow> drivers = driverProfiles.stream()
                .map(d -> new DriverRow(d.getId(), (d.getFirstName() + " " + d.getLastName()).trim(), d.getPhone(),
                        d.getVerificationStatus(), d.getVehicleId() == null ? null : vehicleNumbers.get(d.getVehicleId())))
                .toList();

        long vehicleCount = vehicleRepository.countBySchoolId(schoolId);
        long studentCount = studentRepository.countBySchoolId(schoolId);
        long subscribedCount = studentRepository.countBySchoolIdAndChildIdIsNotNull(schoolId);
        long openGrievances = grievanceRepository.countBySchoolIdAndStatusIn(schoolId, OPEN_GRIEVANCE_STATUSES);
        var recentGrievances = grievanceService.search(schoolId, null, null, 0, RECENT_GRIEVANCES).content();

        return new SchoolOverviewResponse(school, drivers, vehicleCount, studentCount, subscribedCount, openGrievances,
                recentGrievances);
    }
}
