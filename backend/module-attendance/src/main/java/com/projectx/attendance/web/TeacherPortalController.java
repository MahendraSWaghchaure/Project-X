package com.projectx.attendance.web;

import com.projectx.attendance.dto.TimetableDtos.DaySchedule;
import com.projectx.attendance.dto.TimetableDtos.PeriodRequest;
import com.projectx.attendance.dto.TimetableDtos.PeriodResponse;
import com.projectx.attendance.dto.TimetableDtos.TeacherAlert;
import com.projectx.attendance.dto.TimetableDtos.TeacherClassroom;
import com.projectx.attendance.dto.TimetableGenerateDtos.GenerateTimetableRequest;
import com.projectx.attendance.dto.TimetableGenerateDtos.TimetableGenerationResponse;
import com.projectx.attendance.service.SchoolTime;
import com.projectx.attendance.service.TeacherAlertService;
import com.projectx.attendance.service.TimetableGeneratorService;
import com.projectx.attendance.service.TimetableService;
import com.projectx.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Timetable periods (school-admin CRUD) and the TEACHER's own views: periods, classes, alerts. The {@code /teachers/me/**}
 * endpoints are tenant-safe by construction - they only ever read rows keyed by the caller's own user id.
 */
@RestController
public class TeacherPortalController {

    private static final String ADMIN = "hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')";

    private final TimetableService timetableService;
    private final TeacherAlertService alertService;
    private final TimetableGeneratorService timetableGeneratorService;

    public TeacherPortalController(TimetableService timetableService, TeacherAlertService alertService,
                                   TimetableGeneratorService timetableGeneratorService) {
        this.timetableService = timetableService;
        this.alertService = alertService;
        this.timetableGeneratorService = timetableGeneratorService;
    }

    /** C5: dry run - computes the plan, writes nothing. */
    @PostMapping("/schools/{schoolId}/classrooms/timetable/preview")
    @PreAuthorize(ADMIN)
    public ApiResponse<TimetableGenerationResponse> previewTimetable(@PathVariable Long schoolId,
                                                                       @Valid @RequestBody GenerateTimetableRequest request) {
        return ApiResponse.ok(timetableGeneratorService.preview(schoolId, request));
    }

    /** C5: runs the same algorithm as preview and writes every successfully-planned slot as a TimetablePeriod. */
    @PostMapping("/schools/{schoolId}/classrooms/timetable/generate")
    @PreAuthorize(ADMIN)
    public ApiResponse<TimetableGenerationResponse> generateTimetable(@PathVariable Long schoolId,
                                                                        @Valid @RequestBody GenerateTimetableRequest request) {
        return ApiResponse.ok(timetableGeneratorService.generate(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/classrooms/{classroomId}/periods")
    @PreAuthorize(ADMIN)
    public ApiResponse<List<PeriodResponse>> classroomPeriods(@PathVariable Long schoolId, @PathVariable Long classroomId) {
        return ApiResponse.ok(timetableService.listForClassroom(schoolId, classroomId));
    }

    @PostMapping("/schools/{schoolId}/classrooms/{classroomId}/periods")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public ApiResponse<PeriodResponse> addPeriod(@PathVariable Long schoolId, @PathVariable Long classroomId,
                                                  @Valid @RequestBody PeriodRequest request) {
        return ApiResponse.ok(timetableService.create(schoolId, classroomId, request));
    }

    @DeleteMapping("/schools/{schoolId}/periods/{periodId}")
    @PreAuthorize(ADMIN)
    public ApiResponse<Void> deletePeriod(@PathVariable Long schoolId, @PathVariable Long periodId) {
        timetableService.delete(schoolId, periodId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/schools/{schoolId}/teachers/{teacherId}/periods")
    @PreAuthorize(ADMIN)
    public ApiResponse<List<PeriodResponse>> teacherPeriods(@PathVariable Long schoolId, @PathVariable Long teacherId) {
        return ApiResponse.ok(timetableService.listForTeacher(schoolId, teacherId));
    }

    @GetMapping("/teachers/me/periods")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<DaySchedule>> myPeriods() {
        return ApiResponse.ok(timetableService.myPeriods());
    }

    @GetMapping("/teachers/me/classrooms")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<TeacherClassroom>> myClassrooms() {
        return ApiResponse.ok(timetableService.myClassrooms());
    }

    @GetMapping("/teachers/me/alerts")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<TeacherAlert>> myAlerts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(alertService.listMine(date != null ? date : SchoolTime.today()));
    }
}
