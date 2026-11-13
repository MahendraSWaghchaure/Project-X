package com.projectx.attendance.service;

import com.projectx.attendance.domain.AttendanceCorrectionRequest;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.domain.Classroom;
import com.projectx.attendance.domain.CorrectionDecision;
import com.projectx.attendance.domain.CorrectionStatus;
import com.projectx.attendance.domain.Student;
import com.projectx.attendance.dto.AttendanceCorrectionDtos.CorrectionResponse;
import com.projectx.attendance.dto.AttendanceCorrectionDtos.CreateCorrectionRequest;
import com.projectx.attendance.dto.AttendanceCorrectionDtos.DecisionRequest;
import com.projectx.attendance.repo.AttendanceCorrectionRequestRepository;
import com.projectx.attendance.repo.ClassroomRepository;
import com.projectx.attendance.repo.StudentRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.common.web.PageResponse;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * C3: the correction-request workflow that sits behind {@link AttendanceService#bulk}'s teacher
 * lock. A TEACHER who can no longer directly edit a submitted day's row files one of these;
 * a SCHOOL_ADMIN (or PLATFORM_ADMIN) approves (applying the change through the exact same upsert
 * path a direct admin edit would use, then re-syncing Arrivals - see {@link #decide}) or rejects.
 *
 * <p>Not class-level {@code @Transactional}, same reasoning as AttendanceService: {@link #decide}
 * deliberately commits the decision + attendance write together, then syncs Arrivals afterward in
 * their own retrying transaction, so a contended arrival can never roll back an admin's decision.
 */
@Service
public class AttendanceCorrectionService {

    private final AttendanceCorrectionRequestRepository correctionRequestRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final TeacherScope teacherScope;
    private final AttendanceService attendanceService;
    private final ArrivalService arrivalService;
    private final SummaryCache summaryCache;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher eventPublisher;

    public AttendanceCorrectionService(AttendanceCorrectionRequestRepository correctionRequestRepository,
                                       ClassroomRepository classroomRepository, StudentRepository studentRepository,
                                       TeacherScope teacherScope, AttendanceService attendanceService,
                                       ArrivalService arrivalService, SummaryCache summaryCache, JdbcTemplate jdbc,
                                       PlatformTransactionManager transactionManager, ApplicationEventPublisher eventPublisher) {
        this.correctionRequestRepository = correctionRequestRepository;
        this.classroomRepository = classroomRepository;
        this.studentRepository = studentRepository;
        this.teacherScope = teacherScope;
        this.attendanceService = attendanceService;
        this.arrivalService = arrivalService;
        this.summaryCache = summaryCache;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CorrectionResponse create(Long schoolId, CreateCorrectionRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (request.date().isAfter(SchoolTime.today())) {
            throw new IllegalArgumentException("Cannot request a correction for a future date");
        }
        Classroom classroom = classroomRepository.findByIdAndSchoolId(request.classroomId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Classroom", request.classroomId()));
        // Only the class teacher (or an admin) may request/see corrections for this class - same gate as marking attendance.
        teacherScope.requireClassTeacher(schoolId, request.classroomId());
        Student student = studentRepository.findByIdAndSchoolId(request.studentId(), schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", request.studentId()));
        if (!Objects.equals(student.getClassroomId(), classroom.getId())) {
            throw new IllegalArgumentException("That student is not in this classroom");
        }
        if (correctionRequestRepository.existsByClassroomIdAndStudentIdAndDateAndStatus(
                request.classroomId(), request.studentId(), request.date(), CorrectionStatus.PENDING)) {
            throw new ConflictException("A correction request for this student on this date is already pending");
        }
        AttendanceStatus previous = currentStoredStatus(request.studentId(), request.date());
        if (previous == request.requestedStatus()) {
            throw new IllegalArgumentException("The requested status is already what's recorded for this day");
        }
        CurrentUser actor = CurrentUserContext.get();
        AttendanceCorrectionRequest entity = new AttendanceCorrectionRequest(schoolId, request.classroomId(), request.date(),
                request.studentId(), previous, request.requestedStatus(), request.reason().trim(), actor.userId());
        entity = correctionRequestRepository.save(entity);
        eventPublisher.publishEvent(AuditEvent.of("ATTENDANCE_CORRECTION_REQUESTED", "ATTENDANCE_CORRECTION", entity.getId(),
                schoolId, "student=" + request.studentId() + " date=" + request.date() + " from=" + previous
                        + " to=" + request.requestedStatus()));
        return toResponse(entity, classroom.displayName(), studentName(student));
    }

    @Transactional(readOnly = true)
    public PageResponse<CorrectionResponse> list(Long schoolId, CorrectionStatus status, Integer page, Integer size) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        CurrentUser actor = CurrentUserContext.get();
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), StudentService.clampSize(size),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        // A teacher sees only their own requests (badge/review on their own class); an admin sees every request of the school.
        Page<AttendanceCorrectionRequest> result = actor.role() == Role.TEACHER
                ? (status != null
                        ? correctionRequestRepository.findBySchoolIdAndRequestedByUserIdAndStatus(schoolId, actor.userId(), status, pageable)
                        : correctionRequestRepository.findBySchoolIdAndRequestedByUserId(schoolId, actor.userId(), pageable))
                : (status != null
                        ? correctionRequestRepository.findBySchoolIdAndStatus(schoolId, status, pageable)
                        : correctionRequestRepository.findBySchoolId(schoolId, pageable));

        Set<Long> classroomIds = new HashSet<>();
        Set<Long> studentIds = new HashSet<>();
        for (AttendanceCorrectionRequest r : result.getContent()) {
            classroomIds.add(r.getClassroomId());
            studentIds.add(r.getStudentId());
        }
        Map<Long, String> classroomNames = new HashMap<>();
        for (Classroom c : classroomRepository.findAllById(classroomIds)) {
            classroomNames.put(c.getId(), c.displayName());
        }
        Map<Long, String> studentNames = new HashMap<>();
        for (Student s : studentRepository.findAllById(studentIds)) {
            studentNames.put(s.getId(), studentName(s));
        }
        return PageResponse.from(result.map(r -> toResponse(r, classroomNames.get(r.getClassroomId()), studentNames.get(r.getStudentId()))));
    }

    /** SCHOOL_ADMIN/PLATFORM_ADMIN only (controller-gated). APPROVE re-syncs any affected Arrival afterward. */
    public CorrectionResponse decide(Long schoolId, Long requestId, DecisionRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        CurrentUser actor = CurrentUserContext.get();
        if (request.decision() == CorrectionDecision.REJECT && blankToNull(request.note()) == null) {
            throw new IllegalArgumentException("note is required when rejecting a correction request");
        }
        AttendanceCorrectionRequest[] decided = new AttendanceCorrectionRequest[1];
        tx.execute(txStatus -> {
            AttendanceCorrectionRequest entity = correctionRequestRepository.findByIdAndSchoolId(requestId, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("AttendanceCorrectionRequest", requestId));
            if (entity.getStatus() != CorrectionStatus.PENDING) {
                throw new ConflictException("This correction request was already decided");
            }
            boolean approve = request.decision() == CorrectionDecision.APPROVE;
            if (approve) {
                // Same validated upsert path a direct admin edit of the sheet would use.
                attendanceService.upsertForCorrection(schoolId, entity.getClassroomId(), entity.getDate(), entity.getStudentId(),
                        entity.getRequestedStatus(), actor.userId());
            }
            entity.decide(approve ? CorrectionStatus.APPROVED : CorrectionStatus.REJECTED, actor.userId(), blankToNull(request.note()));
            correctionRequestRepository.save(entity);
            eventPublisher.publishEvent(AuditEvent.of(approve ? "ATTENDANCE_CORRECTION_APPROVED" : "ATTENDANCE_CORRECTION_REJECTED",
                    "ATTENDANCE_CORRECTION", entity.getId(), schoolId,
                    "student=" + entity.getStudentId() + " date=" + entity.getDate() + " requestedStatus=" + entity.getRequestedStatus()));
            decided[0] = entity;
            return null;
        });

        AttendanceCorrectionRequest entity = decided[0];
        if (entity.getStatus() == CorrectionStatus.APPROVED) {
            arrivalService.syncSchoolConfirmations(Map.of(entity.getStudentId(), entity.getRequestedStatus()), entity.getDate());
            summaryCache.evict(schoolId, entity.getDate());
        }
        return toResponse(entity);
    }

    private AttendanceStatus currentStoredStatus(Long studentId, LocalDate date) {
        return jdbc.query("SELECT status FROM school_attendance WHERE student_id = ? AND attendance_date = ?",
                        (rs, i) -> AttendanceStatus.valueOf(rs.getString(1)), studentId, Date.valueOf(date))
                .stream().findFirst().orElse(null);
    }

    private CorrectionResponse toResponse(AttendanceCorrectionRequest r) {
        String classroomName = classroomRepository.findById(r.getClassroomId()).map(Classroom::displayName).orElse(null);
        String studentName = studentRepository.findById(r.getStudentId()).map(this::studentName).orElse(null);
        return toResponse(r, classroomName, studentName);
    }

    private CorrectionResponse toResponse(AttendanceCorrectionRequest r, String classroomName, String studentName) {
        return CorrectionResponse.from(r, classroomName, studentName);
    }

    private String studentName(Student s) {
        return (s.getFirstName() + " " + s.getLastName()).trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
