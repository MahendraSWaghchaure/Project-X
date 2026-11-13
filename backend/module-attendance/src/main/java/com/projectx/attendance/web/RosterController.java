package com.projectx.attendance.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectx.attendance.domain.Subscription;
import com.projectx.attendance.dto.ClassroomDtos.AssignClassTeacherRequest;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomRequest;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomResponse;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomSummary;
import com.projectx.attendance.dto.ClassroomDtos.UpdateClassroomRequest;
import com.projectx.attendance.dto.StudentDtos.DirectoryRow;
import com.projectx.attendance.dto.StudentDtos.ImportResult;
import com.projectx.attendance.dto.StudentDtos.LinkChildRequest;
import com.projectx.attendance.dto.StudentDtos.StudentRequest;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.attendance.dto.StudentDtos.StudentSearchResult;
import com.projectx.attendance.service.ClassroomService;
import com.projectx.attendance.service.StudentService;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Classrooms and the student roster. Reads are open to TEACHER; roster/classroom mutations are
 * admin-only (a teacher marks attendance and records progress, they do not edit who is on the roster
 * or touch guardian consent). Tenant scoping is enforced in the services via
 * CurrentUserContext.requireSchoolAccess.
 */
@RestController
public class RosterController {

    private static final String STAFF = "hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')";
    private static final String ADMIN = "hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')";

    private final ClassroomService classroomService;
    private final StudentService studentService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public RosterController(ClassroomService classroomService, StudentService studentService, ObjectMapper objectMapper,
                            Validator validator) {
        this.classroomService = classroomService;
        this.studentService = studentService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @GetMapping("/schools/{schoolId}/classrooms")
    @PreAuthorize(STAFF)
    public ApiResponse<List<ClassroomResponse>> listClassrooms(@PathVariable Long schoolId) {
        return ApiResponse.ok(classroomService.list(schoolId));
    }

    /** The classrooms hub: one row per class with class teacher, total students and today's present/absent counts (a teacher gets only their own classes). */
    @GetMapping("/schools/{schoolId}/classrooms/summary")
    @PreAuthorize(STAFF)
    public ApiResponse<List<ClassroomSummary>> classroomSummary(
            @PathVariable Long schoolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(classroomService.summary(schoolId, date));
    }

    @PatchMapping("/schools/{schoolId}/classrooms/{classroomId}")
    @PreAuthorize(ADMIN)
    public ApiResponse<ClassroomResponse> updateClassroom(@PathVariable Long schoolId, @PathVariable Long classroomId,
                                                           @Valid @RequestBody UpdateClassroomRequest request) {
        return ApiResponse.ok(classroomService.update(schoolId, classroomId, request));
    }

    /** Set (or, with a null teacherId, clear) the class teacher. A teacher can be class teacher of at most one class (409 otherwise). */
    @PutMapping("/schools/{schoolId}/classrooms/{classroomId}/class-teacher")
    @PreAuthorize(ADMIN)
    public ApiResponse<ClassroomResponse> assignClassTeacher(@PathVariable Long schoolId, @PathVariable Long classroomId,
                                                              @RequestBody AssignClassTeacherRequest request) {
        return ApiResponse.ok(classroomService.assignClassTeacher(schoolId, classroomId, request.teacherId()));
    }

    @PostMapping("/schools/{schoolId}/classrooms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public ApiResponse<ClassroomResponse> createClassroom(@PathVariable Long schoolId, @Valid @RequestBody ClassroomRequest request) {
        return ApiResponse.ok(classroomService.create(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/students")
    @PreAuthorize(STAFF)
    public ApiResponse<PageResponse<StudentResponse>> listStudents(
            @PathVariable Long schoolId,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) Subscription subscription,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(studentService.list(schoolId, classroomId, subscription, q, page, size));
    }

    /** C2: "Students & Guardians" directory - cross-class, name search + classroom filter, school-admin only. */
    @GetMapping("/schools/{schoolId}/students/directory")
    @PreAuthorize(ADMIN)
    public ApiResponse<PageResponse<DirectoryRow>> studentsDirectory(
            @PathVariable Long schoolId,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(studentService.directory(schoolId, classroomId, q, page, size));
    }

    /** E4: parent's "pick the other student" grievance dialog - scoped to a school their own child attends, minimal fields only. */
    @GetMapping("/schools/{schoolId}/students/search")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<List<StudentSearchResult>> searchStudentsForParent(@PathVariable Long schoolId, @RequestParam String q) {
        return ApiResponse.ok(studentService.searchForParent(schoolId, q));
    }

    @PostMapping("/schools/{schoolId}/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public ApiResponse<StudentResponse> createStudent(@PathVariable Long schoolId, @Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.create(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/students/{studentId}")
    @PreAuthorize(STAFF)
    public ApiResponse<StudentResponse> getStudent(@PathVariable Long schoolId, @PathVariable Long studentId) {
        return ApiResponse.ok(studentService.get(schoolId, studentId));
    }

    @PutMapping("/schools/{schoolId}/students/{studentId}")
    @PreAuthorize(ADMIN)
    public ApiResponse<StudentResponse> updateStudent(@PathVariable Long schoolId, @PathVariable Long studentId,
                                                       @Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok(studentService.update(schoolId, studentId, request));
    }

    /**
     * Body: {@code {"rows":[StudentRequest,...]}} (<= 500). A bare JSON array, or {@code {"students":[...]}},
     * is accepted too. Rate limited (RateLimitFilter) and all-or-nothing.
     */
    @PostMapping("/schools/{schoolId}/students/import")
    @PreAuthorize(ADMIN)
    public ApiResponse<ImportResult> importStudents(@PathVariable Long schoolId, @RequestBody Object body) {
        // Object (not JsonNode): Spring MVC here binds with Jackson 3, this module's ObjectMapper is Jackson 2 - plain maps/lists bridge both.
        Object rowsNode = body instanceof Map<?, ?> map ? (map.containsKey("rows") ? map.get("rows") : map.get("students")) : body;
        if (!(rowsNode instanceof List<?> rawRows)) {
            throw new IllegalArgumentException("Body must be {\"rows\": [...]} (or a bare array) of students");
        }
        if (rawRows.size() > StudentService.MAX_IMPORT_ROWS) {
            throw new IllegalArgumentException("At most " + StudentService.MAX_IMPORT_ROWS + " rows per import (got " + rawRows.size() + ")");
        }
        List<StudentRequest> rows = new ArrayList<>(rawRows.size());
        for (int i = 0; i < rawRows.size(); i++) {
            StudentRequest row;
            try {
                row = objectMapper.convertValue(rawRows.get(i), StudentRequest.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Row " + (i + 1) + " is malformed");
            }
            Set<ConstraintViolation<StudentRequest>> violations = validator.validate(row);
            if (!violations.isEmpty()) {
                ConstraintViolation<StudentRequest> v = violations.iterator().next();
                throw new IllegalArgumentException("Row " + (i + 1) + ": " + v.getPropertyPath() + " " + v.getMessage());
            }
            rows.add(row);
        }
        return ApiResponse.ok(studentService.importStudents(schoolId, rows));
    }

    @PostMapping("/schools/{schoolId}/students/{studentId}/link-child")
    @PreAuthorize(ADMIN)
    public ApiResponse<StudentResponse> linkChild(@PathVariable Long schoolId, @PathVariable Long studentId,
                                                   @Valid @RequestBody LinkChildRequest request) {
        return ApiResponse.ok(studentService.linkChild(schoolId, studentId, request.childId()));
    }

    @DeleteMapping("/schools/{schoolId}/students/{studentId}/link-child")
    @PreAuthorize(ADMIN)
    public ApiResponse<StudentResponse> unlinkChild(@PathVariable Long schoolId, @PathVariable Long studentId) {
        return ApiResponse.ok(studentService.unlinkChild(schoolId, studentId));
    }
}
