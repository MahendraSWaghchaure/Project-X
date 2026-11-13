package com.projectx.attendance.service;

import com.projectx.attendance.domain.Classroom;
import com.projectx.attendance.domain.TeacherProfile;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomRequest;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomResponse;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomSummary;
import com.projectx.attendance.dto.ClassroomDtos.UpdateClassroomRequest;
import com.projectx.attendance.repo.ClassroomRepository;
import com.projectx.attendance.repo.TeacherProfileRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classrooms: the school-admin hub. Counts (students, present, absent) are SQL aggregates - never a student list fetched to
 * be counted in Java. A TEACHER only ever sees the classes they are class teacher of or lecture in.
 */
@Service
@Transactional
public class ClassroomService {

    /** A school has tens of classrooms; this cap only bounds abuse. */
    static final int MAX_CLASSROOMS_LISTED = 200;

    private final ClassroomRepository classroomRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final TeacherScope teacherScope;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventPublisher;

    public ClassroomService(ClassroomRepository classroomRepository, TeacherProfileRepository teacherProfileRepository,
                            UserAccountRepository userAccountRepository, TeacherScope teacherScope, JdbcTemplate jdbc,
                            ApplicationEventPublisher eventPublisher) {
        this.classroomRepository = classroomRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.teacherScope = teacherScope;
        this.jdbc = jdbc;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> list(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        var sort = Sort.by("name", "section", "id");
        List<Classroom> classrooms;
        if (TeacherScope.isTeacher()) {
            Set<Long> related = teacherScope.relatedClassroomIds(schoolId);
            classrooms = related.isEmpty() ? List.of()
                    : classroomRepository.findBySchoolIdAndIdIn(schoolId, related, PageRequest.of(0, MAX_CLASSROOMS_LISTED, sort));
        } else {
            classrooms = classroomRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_CLASSROOMS_LISTED, sort));
        }
        return toResponses(schoolId, classrooms);
    }

    public ClassroomResponse create(Long schoolId, ClassroomRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        String section = blankToNull(request.section());
        Classroom classroom = new Classroom(schoolId, request.name().trim(), section);
        if (request.classTeacherId() != null) {
            requireAssignableTeacher(schoolId, request.classTeacherId(), null);
            classroom.setClassTeacherId(request.classTeacherId());
        }
        try {
            classroom = classroomRepository.saveAndFlush(classroom);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(request.classTeacherId() != null
                    ? "Classroom already exists, or that teacher is already a class teacher: " + request.name() + (section == null ? "" : " " + section)
                    : "Classroom already exists: " + request.name() + (section == null ? "" : " " + section));
        }
        eventPublisher.publishEvent(AuditEvent.of("CLASSROOM_CREATED", "CLASSROOM", classroom.getId(), schoolId,
                classroom.displayName() + (classroom.getClassTeacherId() == null ? "" : " classTeacher=" + classroom.getClassTeacherId())));
        return toResponses(schoolId, List.of(classroom)).get(0);
    }

    public ClassroomResponse update(Long schoolId, Long classroomId, UpdateClassroomRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Classroom classroom = requireClassroom(schoolId, classroomId);
        if (request.version() != null && !request.version().equals(classroom.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Classroom.class, classroomId);
        }
        classroom.rename(request.name().trim(), blankToNull(request.section()));
        try {
            classroomRepository.saveAndFlush(classroom);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Classroom already exists: " + request.name());
        }
        return toResponses(schoolId, List.of(classroom)).get(0);
    }

    /**
     * Makes {@code teacherId} the class teacher of the classroom (null removes it). A teacher is class teacher of at most one
     * classroom: enforced by a DB-unique index, and pre-checked here for a clear 409.
     */
    public ClassroomResponse assignClassTeacher(Long schoolId, Long classroomId, Long teacherId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Classroom classroom = requireClassroom(schoolId, classroomId);
        applyClassTeacher(schoolId, classroom, teacherId);
        return toResponses(schoolId, List.of(classroom)).get(0);
    }

    /** Shared by the classroom endpoint and the teacher invite/edit forms; flushes so the unique index reports a conflict now. */
    public void applyClassTeacher(Long schoolId, Classroom classroom, Long teacherId) {
        if (Objects.equals(classroom.getClassTeacherId(), teacherId)) {
            return;
        }
        if (teacherId != null) {
            requireAssignableTeacher(schoolId, teacherId, classroom.getId());
        }
        Long before = classroom.getClassTeacherId();
        classroom.setClassTeacherId(teacherId);
        try {
            classroomRepository.saveAndFlush(classroom);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("That teacher is already the class teacher of another class");
        }
        eventPublisher.publishEvent(AuditEvent.of("CLASS_TEACHER_CHANGED", "CLASSROOM", classroom.getId(), schoolId,
                "classTeacher " + before + " -> " + teacherId));
    }

    /** Teacher must be a TEACHER account of this school, and not already class teacher of a different classroom. */
    private void requireAssignableTeacher(Long schoolId, Long teacherId, Long exceptClassroomId) {
        UserAccount teacher = userAccountRepository.findById(teacherId).orElse(null);
        if (teacher == null || teacher.getRole() != Role.TEACHER || !Objects.equals(teacher.getSchoolId(), schoolId)) {
            // Same answer as a missing teacher: never confirm another tenant's user id exists.
            throw ResourceNotFoundException.of("Teacher", teacherId);
        }
        classroomRepository.findByClassTeacherId(teacherId).ifPresent(other -> {
            if (!Objects.equals(other.getId(), exceptClassroomId)) {
                throw new ConflictException("That teacher is already the class teacher of " + other.displayName());
            }
        });
    }

    /**
     * The classrooms hub table for a date (default today, India time): one SQL statement with two grouped sub-selects,
     * regardless of how many classes or students the school has.
     */
    @Transactional(readOnly = true)
    public List<ClassroomSummary> summary(Long schoolId, LocalDate date) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        LocalDate day = date != null ? date : SchoolTime.today();
        List<Object> args = new ArrayList<>(List.of(schoolId, schoolId, Date.valueOf(day), schoolId));
        String restrict = "";
        if (TeacherScope.isTeacher()) {
            Set<Long> related = teacherScope.relatedClassroomIds(schoolId);
            if (related.isEmpty()) {
                return List.of();
            }
            restrict = " AND c.id = ANY(?::bigint[])";
            args.add("{" + related.stream().map(String::valueOf).collect(Collectors.joining(",")) + "}");
        }
        List<Object[]> rows = jdbc.query("""
                SELECT c.id, c.name, c.section, c.class_teacher_id,
                       COALESCE(st.total, 0), COALESCE(att.present, 0), COALESCE(att.late, 0), COALESCE(att.absent, 0)
                  FROM classrooms c
                  LEFT JOIN (SELECT classroom_id, count(*) AS total FROM students WHERE school_id = ? GROUP BY classroom_id) st
                         ON st.classroom_id = c.id
                  LEFT JOIN (SELECT s.classroom_id,
                                    count(*) FILTER (WHERE a.status = 'PRESENT') AS present,
                                    count(*) FILTER (WHERE a.status = 'LATE') AS late,
                                    count(*) FILTER (WHERE a.status = 'ABSENT') AS absent
                               FROM school_attendance a JOIN students s ON s.id = a.student_id
                              WHERE a.school_id = ? AND a.attendance_date = ?
                              GROUP BY s.classroom_id) att ON att.classroom_id = c.id
                 WHERE c.school_id = ?""" + restrict + """

                 ORDER BY c.name, c.section NULLS FIRST, c.id""" + " LIMIT " + MAX_CLASSROOMS_LISTED,
                (rs, i) -> new Object[] {rs.getLong(1), rs.getString(2), rs.getString(3), rs.getObject(4) == null ? null : rs.getLong(4),
                        rs.getLong(5), rs.getLong(6), rs.getLong(7), rs.getLong(8)},
                args.toArray());
        Map<Long, String> teacherNames = teacherNames(rows.stream().map(r -> (Long) r[3]).filter(Objects::nonNull).toList());
        List<ClassroomSummary> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            long total = (Long) r[4];
            long present = (Long) r[5];
            long late = (Long) r[6];
            long absent = (Long) r[7];
            Long teacherId = (Long) r[3];
            out.add(new ClassroomSummary((Long) r[0], (String) r[1], (String) r[2], teacherId,
                    teacherId == null ? null : teacherNames.get(teacherId), total, present + late, absent, late,
                    Math.max(0, total - present - late - absent)));
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private Classroom requireClassroom(Long schoolId, Long classroomId) {
        return classroomRepository.findByIdAndSchoolId(classroomId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Classroom", classroomId));
    }

    /** classTeacherName and studentCount for a batch of classrooms: one GROUP BY count + one profile lookup. */
    private List<ClassroomResponse> toResponses(Long schoolId, List<Classroom> classrooms) {
        if (classrooms.isEmpty()) {
            return List.of();
        }
        String ids = "{" + classrooms.stream().map(c -> String.valueOf(c.getId())).collect(Collectors.joining(",")) + "}";
        Map<Long, Long> counts = new HashMap<>();
        jdbc.query("SELECT classroom_id, count(*) FROM students WHERE school_id = ? AND classroom_id = ANY(?::bigint[]) GROUP BY classroom_id",
                rs -> {
                    counts.put(rs.getLong(1), rs.getLong(2));
                }, schoolId, ids);
        Map<Long, String> names = teacherNames(classrooms.stream().map(Classroom::getClassTeacherId).filter(Objects::nonNull).toList());
        return classrooms.stream().map(c -> ClassroomResponse.from(c,
                c.getClassTeacherId() == null ? null : names.get(c.getClassTeacherId()), counts.getOrDefault(c.getId(), 0L))).toList();
    }

    private Map<Long, String> teacherNames(Collection<Long> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        return teacherProfileRepository.findByUserIdIn(teacherIds).stream()
                .collect(Collectors.toMap(TeacherProfile::getUserId, TeacherProfile::fullName));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
