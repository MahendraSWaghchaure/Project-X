package com.projectx.attendance.service;

import com.projectx.attendance.domain.Classroom;
import com.projectx.attendance.domain.Student;
import com.projectx.attendance.domain.TimetablePeriod;
import com.projectx.attendance.repo.ClassroomRepository;
import com.projectx.attendance.repo.TimetablePeriodRepository;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place that decides what a TEACHER may do in a classroom (server-side; the UI only mirrors it):
 * <ul>
 *   <li>CLASS_TEACHER of X: full student details incl. guardian contacts, attendance, comments and assessments of X;</li>
 *   <li>LECTURER (has periods in Y, is not its class teacher): assessments of Y only - no attendance, no comments, no
 *       guardian contact;</li>
 *   <li>no relation: 403.</li>
 * </ul>
 * Non-teacher callers (school admin / platform admin) are {@link Relation#FULL} - their role gate is the controller's
 * {@code @PreAuthorize}, and the tenant check is {@code requireSchoolAccess}.
 */
@Service
@Transactional(readOnly = true)
public class TeacherScope {

    public enum Relation { FULL, CLASS_TEACHER, LECTURER, NONE }

    /** A teacher has at most a few dozen weekly periods; the cap only bounds abuse. */
    private static final int MAX_PERIODS = 300;

    private final ClassroomRepository classroomRepository;
    private final TimetablePeriodRepository periodRepository;

    public TeacherScope(ClassroomRepository classroomRepository, TimetablePeriodRepository periodRepository) {
        this.classroomRepository = classroomRepository;
        this.periodRepository = periodRepository;
    }

    public static boolean isTeacher() {
        return CurrentUserContext.get().role() == Role.TEACHER;
    }

    /** Relation of the current user to this classroom (404 if the classroom is not in the school). */
    public Relation relation(Long schoolId, Long classroomId) {
        CurrentUser user = CurrentUserContext.get();
        CurrentUserContext.requireSchoolAccess(schoolId);
        Classroom classroom = classroomRepository.findByIdAndSchoolId(classroomId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Classroom", classroomId));
        return relation(user, classroom);
    }

    public Relation relation(CurrentUser user, Classroom classroom) {
        if (user.role() != Role.TEACHER) {
            return Relation.FULL;
        }
        if (user.userId().equals(classroom.getClassTeacherId())) {
            return Relation.CLASS_TEACHER;
        }
        return periodRepository.existsByTeacherIdAndClassroomId(user.userId(), classroom.getId()) ? Relation.LECTURER : Relation.NONE;
    }

    /** Relation of the current user to the class the student sits in (a student with no class is nobody's: NONE for teachers). */
    public Relation relation(Student student) {
        CurrentUser user = CurrentUserContext.get();
        if (user.role() != Role.TEACHER) {
            return Relation.FULL;
        }
        return student.getClassroomId() == null ? Relation.NONE : relation(student.getSchoolId(), student.getClassroomId());
    }

    /** Full details / attendance / comments: admins or the class teacher of this classroom. */
    public void requireClassTeacher(Long schoolId, Long classroomId) {
        Relation r = relation(schoolId, classroomId);
        if (r != Relation.FULL && r != Relation.CLASS_TEACHER) {
            throw new ForbiddenOperationException("Only the class teacher of this class can do that");
        }
    }

    public void requireClassTeacher(Student student) {
        Relation r = relation(student);
        if (r != Relation.FULL && r != Relation.CLASS_TEACHER) {
            throw new ForbiddenOperationException("Only the class teacher of this student's class can do that");
        }
    }

    /** Assessments: admins, the class teacher, or a lecturer of the class. */
    public Relation requireAnyRelation(Student student) {
        Relation r = relation(student);
        if (r == Relation.NONE) {
            throw new ForbiddenOperationException("You do not teach this student's class");
        }
        return r;
    }

    /** Classroom ids the current teacher is class teacher of (0 or 1) or lectures in. Empty for non-teachers - callers branch on {@link #isTeacher()}. */
    public Set<Long> relatedClassroomIds(Long schoolId) {
        CurrentUser user = CurrentUserContext.get();
        CurrentUserContext.requireSchoolAccess(schoolId);
        Set<Long> ids = new HashSet<>();
        classroomRepository.findByClassTeacherId(user.userId()).ifPresent(c -> ids.add(c.getId()));
        for (TimetablePeriod p : periodRepository.findByTeacherId(user.userId(), PageRequest.of(0, MAX_PERIODS))) {
            ids.add(p.getClassroomId());
        }
        return ids;
    }

    /** The classroom the current teacher is class teacher of, if any. */
    public java.util.Optional<Long> classTeacherClassroomId() {
        return classroomRepository.findByClassTeacherId(CurrentUserContext.get().userId()).map(Classroom::getId);
    }
}
