package com.projectx.attendance.service;

import com.projectx.attendance.domain.Classroom;
import com.projectx.attendance.domain.TeacherProfile;
import com.projectx.attendance.domain.TimetablePeriod;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomResponse;
import com.projectx.attendance.dto.TimetableDtos.DaySchedule;
import com.projectx.attendance.dto.TimetableDtos.PeriodRequest;
import com.projectx.attendance.dto.TimetableDtos.PeriodResponse;
import com.projectx.attendance.dto.TimetableDtos.TeacherClassroom;
import com.projectx.attendance.repo.ClassroomRepository;
import com.projectx.attendance.repo.TeacherProfileRepository;
import com.projectx.attendance.repo.TimetablePeriodRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Weekly timetable periods (school-admin CRUD) and the teacher's own read-only views of them. */
@Service
@Transactional
public class TimetableService {

    /** A classroom / teacher has at most a few dozen weekly periods; the cap only bounds abuse. */
    static final int MAX_PERIODS_LISTED = 300;

    private final TimetablePeriodRepository periodRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherProfileRepository profileRepository;
    private final UserAccountRepository userAccountRepository;
    private final ClassroomService classroomService;
    private final ApplicationEventPublisher eventPublisher;

    public TimetableService(TimetablePeriodRepository periodRepository, ClassroomRepository classroomRepository,
                            TeacherProfileRepository profileRepository, UserAccountRepository userAccountRepository,
                            ClassroomService classroomService, ApplicationEventPublisher eventPublisher) {
        this.periodRepository = periodRepository;
        this.classroomRepository = classroomRepository;
        this.profileRepository = profileRepository;
        this.userAccountRepository = userAccountRepository;
        this.classroomService = classroomService;
        this.eventPublisher = eventPublisher;
    }

    // ------------------------------------------------------------------ school admin

    @Transactional(readOnly = true)
    public List<PeriodResponse> listForClassroom(Long schoolId, Long classroomId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        requireClassroom(schoolId, classroomId);
        return toResponses(periodRepository.findByClassroomId(classroomId, pageable()));
    }

    @Transactional(readOnly = true)
    public List<PeriodResponse> listForTeacher(Long schoolId, Long teacherId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        requireTeacher(schoolId, teacherId);
        return toResponses(periodRepository.findByTeacherId(teacherId, pageable()));
    }

    public PeriodResponse create(Long schoolId, Long classroomId, PeriodRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Classroom classroom = requireClassroom(schoolId, classroomId);
        requireTeacher(schoolId, request.teacherId());
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (overlaps(periodRepository.findByTeacherIdAndDayOfWeek(request.teacherId(), request.dayOfWeek()), request.startTime(), request.endTime())) {
            throw new ConflictException("That teacher already has a period at this time");
        }
        if (overlaps(periodRepository.findByClassroomIdAndDayOfWeek(classroomId, request.dayOfWeek()), request.startTime(), request.endTime())) {
            throw new ConflictException("This class already has a period at this time");
        }
        TimetablePeriod saved = periodRepository.save(new TimetablePeriod(schoolId, classroomId, request.teacherId(),
                request.subject().trim(), request.dayOfWeek(), request.startTime(), request.endTime()));
        eventPublisher.publishEvent(AuditEvent.of("PERIOD_CREATED", "TIMETABLE_PERIOD", saved.getId(), schoolId,
                classroom.displayName() + " " + saved.getSubject() + " teacher=" + saved.getTeacherId()));
        return toResponses(List.of(saved)).get(0);
    }

    public void delete(Long schoolId, Long periodId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        TimetablePeriod period = periodRepository.findByIdAndSchoolId(periodId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Period", periodId));
        periodRepository.delete(period);
        eventPublisher.publishEvent(AuditEvent.of("PERIOD_DELETED", "TIMETABLE_PERIOD", periodId, schoolId, "teacher=" + period.getTeacherId()));
    }

    // ------------------------------------------------------------------ the teacher's own views

    /** The current teacher's periods grouped by day (only days that have periods), earliest first. */
    @Transactional(readOnly = true)
    public List<DaySchedule> myPeriods() {
        CurrentUser me = CurrentUserContext.get();
        List<PeriodResponse> all = toResponses(periodRepository.findByTeacherId(me.userId(), pageable()));
        Map<Integer, List<PeriodResponse>> byDay = new TreeMap<>();
        for (PeriodResponse p : all) {
            byDay.computeIfAbsent(p.dayOfWeek(), d -> new ArrayList<>()).add(p);
        }
        List<DaySchedule> out = new ArrayList<>();
        byDay.forEach((day, periods) -> {
            periods.sort(Comparator.comparing(PeriodResponse::startTime));
            out.add(new DaySchedule(day, periods));
        });
        return out;
    }

    /** Classes the current teacher is class teacher of (at most one) and lectures in, each with the subjects they teach there. */
    @Transactional(readOnly = true)
    public List<TeacherClassroom> myClassrooms() {
        CurrentUser me = CurrentUserContext.get();
        Long schoolId = me.schoolId();
        List<TimetablePeriod> periods = periodRepository.findByTeacherId(me.userId(), pageable());
        Map<Long, Set<String>> subjectsByClass = new LinkedHashMap<>();
        for (TimetablePeriod p : periods) {
            subjectsByClass.computeIfAbsent(p.getClassroomId(), k -> new LinkedHashSet<>()).add(p.getSubject());
        }
        Classroom classTeacherOf = classroomRepository.findByClassTeacherId(me.userId()).orElse(null);
        Set<Long> ids = new LinkedHashSet<>();
        if (classTeacherOf != null) {
            ids.add(classTeacherOf.getId());
        }
        ids.addAll(subjectsByClass.keySet());
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, ClassroomResponse> classrooms = classroomService.list(schoolId).stream()
                .collect(Collectors.toMap(ClassroomResponse::id, c -> c, (a, b) -> a, LinkedHashMap::new));
        List<TeacherClassroom> out = new ArrayList<>();
        for (Long id : ids) {
            ClassroomResponse c = classrooms.get(id);
            if (c == null) {
                continue;
            }
            boolean classTeacher = classTeacherOf != null && classTeacherOf.getId().equals(id);
            out.add(new TeacherClassroom(c, classTeacher ? "CLASS_TEACHER" : "LECTURER",
                    new ArrayList<>(subjectsByClass.getOrDefault(id, Set.of()))));
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static boolean overlaps(List<TimetablePeriod> existing, LocalTime start, LocalTime end) {
        return existing.stream().anyMatch(p -> start.isBefore(p.getEndTime()) && end.isAfter(p.getStartTime()));
    }

    private static PageRequest pageable() {
        return PageRequest.of(0, MAX_PERIODS_LISTED, Sort.by("dayOfWeek", "startTime", "id"));
    }

    private Classroom requireClassroom(Long schoolId, Long classroomId) {
        return classroomRepository.findByIdAndSchoolId(classroomId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Classroom", classroomId));
    }

    private void requireTeacher(Long schoolId, Long teacherId) {
        UserAccount user = userAccountRepository.findById(teacherId).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER || !Objects.equals(user.getSchoolId(), schoolId)) {
            throw ResourceNotFoundException.of("Teacher", teacherId);
        }
    }

    /** classroom label + teacher name for a batch of periods: 2 queries. */
    private List<PeriodResponse> toResponses(List<TimetablePeriod> periods) {
        if (periods.isEmpty()) {
            return List.of();
        }
        Collection<Long> classIds = periods.stream().map(TimetablePeriod::getClassroomId).collect(Collectors.toSet());
        Collection<Long> teacherIds = periods.stream().map(TimetablePeriod::getTeacherId).collect(Collectors.toSet());
        Map<Long, String> classNames = classroomRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(Classroom::getId, Classroom::displayName));
        Map<Long, String> teacherNames = profileRepository.findByUserIdIn(teacherIds).stream()
                .collect(Collectors.toMap(TeacherProfile::getUserId, TeacherProfile::fullName));
        return periods.stream().map(p -> PeriodResponse.from(p, classNames.get(p.getClassroomId()), teacherNames.get(p.getTeacherId()))).toList();
    }
}
