package com.projectx.attendance.web;

import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.domain.CorrectionStatus;
import com.projectx.attendance.dto.AcademicDtos.AssessmentRequest;
import com.projectx.attendance.dto.AcademicDtos.AssessmentResponse;
import com.projectx.attendance.dto.AcademicDtos.CommentRequest;
import com.projectx.attendance.dto.AcademicDtos.CommentResponse;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.AttendanceCorrectionDtos.CorrectionResponse;
import com.projectx.attendance.dto.AttendanceCorrectionDtos.CreateCorrectionRequest;
import com.projectx.attendance.dto.AttendanceCorrectionDtos.DecisionRequest;
import com.projectx.attendance.dto.AttendanceDtos.AttendanceSummary;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceRequest;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceResult;
import com.projectx.attendance.dto.AttendanceDtos.SheetRow;
import com.projectx.attendance.service.AcademicRecordService;
import com.projectx.attendance.service.ArrivalService;
import com.projectx.attendance.service.AttendanceCorrectionService;
import com.projectx.attendance.service.AttendanceService;
import com.projectx.attendance.service.SchoolTime;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** School-side attendance sheet, arrival board and academic records. Every endpoint is tenant-checked in the service layer. */
@RestController
public class SchoolAttendanceController {

    private static final String STAFF = "hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')";
    private static final String ADMIN = "hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')";

    private final AttendanceService attendanceService;
    private final ArrivalService arrivalService;
    private final AcademicRecordService academicRecordService;
    private final AttendanceCorrectionService attendanceCorrectionService;

    public SchoolAttendanceController(AttendanceService attendanceService, ArrivalService arrivalService,
                                AcademicRecordService academicRecordService, AttendanceCorrectionService attendanceCorrectionService) {
        this.attendanceService = attendanceService;
        this.arrivalService = arrivalService;
        this.academicRecordService = academicRecordService;
        this.attendanceCorrectionService = attendanceCorrectionService;
    }

    @GetMapping("/schools/{schoolId}/attendance")
    @PreAuthorize(STAFF)
    public ApiResponse<List<SheetRow>> sheet(
            @PathVariable Long schoolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long classroomId) {
        return ApiResponse.ok(attendanceService.sheet(schoolId, date != null ? date : SchoolTime.today(), classroomId));
    }

    @PutMapping("/schools/{schoolId}/attendance/bulk")
    @PreAuthorize(STAFF)
    public ApiResponse<BulkAttendanceResult> bulk(@PathVariable Long schoolId, @Valid @RequestBody BulkAttendanceRequest request) {
        return ApiResponse.ok(attendanceService.bulk(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/attendance/summary")
    @PreAuthorize(STAFF)
    public ApiResponse<AttendanceSummary> summary(
            @PathVariable Long schoolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(attendanceService.summary(schoolId, date));
    }

    @GetMapping("/schools/{schoolId}/arrivals")
    @PreAuthorize(ADMIN)
    public ApiResponse<PageResponse<ArrivalResponse>> arrivals(
            @PathVariable Long schoolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ArrivalOutcome outcome,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(arrivalService.listForSchool(schoolId, date, outcome, page, size));
    }

    // ------------------------------------------------------------------ C3 correction requests

    @PostMapping("/schools/{schoolId}/attendance/correction-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF)
    public ApiResponse<CorrectionResponse> requestCorrection(@PathVariable Long schoolId,
                                                              @Valid @RequestBody CreateCorrectionRequest request) {
        return ApiResponse.ok(attendanceCorrectionService.create(schoolId, request));
    }

    /** A TEACHER sees only their own requests; an admin sees every request of the school (badge count). */
    @GetMapping("/schools/{schoolId}/attendance/correction-requests")
    @PreAuthorize(STAFF)
    public ApiResponse<PageResponse<CorrectionResponse>> listCorrectionRequests(
            @PathVariable Long schoolId,
            @RequestParam(required = false) CorrectionStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(attendanceCorrectionService.list(schoolId, status, page, size));
    }

    @PatchMapping("/schools/{schoolId}/attendance/correction-requests/{id}")
    @PreAuthorize(ADMIN)
    public ApiResponse<CorrectionResponse> decideCorrectionRequest(@PathVariable Long schoolId, @PathVariable Long id,
                                                                     @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.ok(attendanceCorrectionService.decide(schoolId, id, request));
    }

    @GetMapping("/schools/{schoolId}/students/{studentId}/assessments")
    @PreAuthorize(STAFF)
    public ApiResponse<List<AssessmentResponse>> assessments(@PathVariable Long schoolId, @PathVariable Long studentId) {
        return ApiResponse.ok(academicRecordService.listAssessments(schoolId, studentId));
    }

    @PostMapping("/schools/{schoolId}/students/{studentId}/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF)
    public ApiResponse<AssessmentResponse> addAssessment(@PathVariable Long schoolId, @PathVariable Long studentId,
                                                          @Valid @RequestBody AssessmentRequest request) {
        return ApiResponse.ok(academicRecordService.addAssessment(schoolId, studentId, request));
    }

    @GetMapping("/schools/{schoolId}/students/{studentId}/comments")
    @PreAuthorize(STAFF)
    public ApiResponse<List<CommentResponse>> comments(@PathVariable Long schoolId, @PathVariable Long studentId) {
        return ApiResponse.ok(academicRecordService.listComments(schoolId, studentId));
    }

    @PostMapping("/schools/{schoolId}/students/{studentId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF)
    public ApiResponse<CommentResponse> addComment(@PathVariable Long schoolId, @PathVariable Long studentId,
                                                    @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(academicRecordService.addComment(schoolId, studentId, request));
    }
}
