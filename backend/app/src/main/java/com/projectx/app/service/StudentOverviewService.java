package com.projectx.app.service;

import com.projectx.app.dto.StudentOverviewResponse;
import com.projectx.app.dto.StudentOverviewResponse.AttendanceDay;
import com.projectx.app.dto.StudentOverviewResponse.Insurance;
import com.projectx.app.dto.StudentOverviewResponse.Transport;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.domain.Student;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.attendance.service.SchoolTime;
import com.projectx.attendance.service.StudentService;
import com.projectx.attendance.service.TeacherScope;
import com.projectx.insurance.repo.InsurancePolicyRepository;
import com.projectx.people.domain.Child;
import com.projectx.people.repo.ChildRepository;
import com.projectx.school.domain.Route;
import com.projectx.school.domain.RouteStop;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.RouteRepository;
import com.projectx.school.repo.RouteStopRepository;
import com.projectx.school.repo.VehicleRepository;
import java.sql.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The student detail page in one request (B6/B12). Scoping is inherited from {@link StudentService#get}: admins and the class
 * teacher see everything, a lecturer only the redacted student, anyone else is refused (403).
 */
@Service
@Transactional(readOnly = true)
public class StudentOverviewService {

    static final int DEFAULT_DAYS = 60;
    static final int MAX_DAYS = 180;

    private final StudentService studentService;
    private final TeacherScope teacherScope;
    private final ChildRepository childRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final VehicleRepository vehicleRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final JdbcTemplate jdbc;

    public StudentOverviewService(StudentService studentService, TeacherScope teacherScope, ChildRepository childRepository,
                                  RouteRepository routeRepository, RouteStopRepository routeStopRepository,
                                  VehicleRepository vehicleRepository, InsurancePolicyRepository insurancePolicyRepository,
                                  JdbcTemplate jdbc) {
        this.studentService = studentService;
        this.teacherScope = teacherScope;
        this.childRepository = childRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.vehicleRepository = vehicleRepository;
        this.insurancePolicyRepository = insurancePolicyRepository;
        this.jdbc = jdbc;
    }

    /** {@code days} = how many days of attendance history (default 60, max 180). */
    public StudentOverviewResponse overview(Long schoolId, Long studentId, Integer days) {
        StudentResponse response = studentService.get(schoolId, studentId); // tenant check + teacher scoping + redaction
        Student entity = studentService.requireStudent(schoolId, studentId);
        if (teacherScope.relation(entity) == TeacherScope.Relation.LECTURER) {
            return new StudentOverviewResponse(response, null, null, null);
        }

        Transport transport = null;
        Insurance insurance = null;
        if (entity.getChildId() != null) {
            Child child = childRepository.findById(entity.getChildId()).filter(c -> Objects.equals(c.getSchoolId(), schoolId)).orElse(null);
            if (child != null) {
                transport = transportOf(child);
                if (child.getInsurancePolicyId() != null) {
                    insurance = insurancePolicyRepository.findById(child.getInsurancePolicyId())
                            .filter(p -> Objects.equals(p.getSchoolId(), schoolId))
                            .map(p -> new Insurance(p.getPolicyNumber(), p.getProvider(), p.getStatus().name(), p.getStartDate(), p.getEndDate()))
                            .orElse(null);
                }
            }
        }

        int window = Math.min(Math.max(days == null ? DEFAULT_DAYS : days, 1), MAX_DAYS);
        List<AttendanceDay> attendance = jdbc.query(
                "SELECT attendance_date, status FROM school_attendance WHERE student_id = ? AND attendance_date >= ? ORDER BY attendance_date DESC LIMIT ?",
                (rs, i) -> new AttendanceDay(rs.getDate(1).toLocalDate(), AttendanceStatus.valueOf(rs.getString(2))),
                studentId, Date.valueOf(SchoolTime.today().minusDays(window)), window + 1);
        return new StudentOverviewResponse(response, transport, insurance, attendance);
    }

    private Transport transportOf(Child child) {
        String routeName = null;
        String vehicleNo = null;
        String stopLabel = null;
        if (child.getRouteId() != null) {
            Route route = routeRepository.findById(child.getRouteId()).orElse(null);
            if (route != null) {
                routeName = route.getName();
                if (route.getVehicleId() != null) {
                    vehicleNo = vehicleRepository.findById(route.getVehicleId()).map(Vehicle::getRegistrationNumber).orElse(null);
                }
            }
        }
        if (child.getRouteStopId() != null) {
            stopLabel = routeStopRepository.findById(child.getRouteStopId()).map(RouteStop::getLabel).orElse(null);
        }
        return new Transport(child.getId(), routeName, stopLabel, vehicleNo);
    }
}
