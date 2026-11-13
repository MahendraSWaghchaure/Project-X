package com.projectx.attendance.service;

import com.projectx.attendance.domain.Classroom;
import com.projectx.attendance.domain.TeacherProfile;
import com.projectx.attendance.domain.TimetablePeriod;
import com.projectx.attendance.dto.TimetableDtos.PeriodRequest;
import com.projectx.attendance.dto.TimetableGenerateDtos.BreakInput;
import com.projectx.attendance.dto.TimetableGenerateDtos.GenerateTimetableRequest;
import com.projectx.attendance.dto.TimetableGenerateDtos.GeneratedPeriod;
import com.projectx.attendance.dto.TimetableGenerateDtos.SubjectLoadInput;
import com.projectx.attendance.dto.TimetableGenerateDtos.TeacherInput;
import com.projectx.attendance.dto.TimetableGenerateDtos.TimetableGenerationResponse;
import com.projectx.attendance.dto.TimetableGenerateDtos.UnfilledSlot;
import com.projectx.attendance.repo.ClassroomRepository;
import com.projectx.attendance.repo.TeacherProfileRepository;
import com.projectx.attendance.repo.TimetablePeriodRepository;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C5: the automatic timetable generator. A greedy, single-pass (no backtracking) constraint
 * scheduler - deliberately NOT an exact optimizer. It processes classes in request order and, for
 * each one, days in order and period slots in chronological order, making the locally-best choice
 * at each slot and never revisiting an earlier decision. That means an early, low-priority
 * assignment can occasionally block a later, more important one from fitting; when that happens the
 * slot (or, if a whole subject's weekly demand can't fit anywhere in the week, the shortfall) is
 * reported in the response's {@code unfilled} list - never silently dropped and never an exception.
 *
 * <p><b>Rules implemented</b> (see PLAN-phase3-polish-geo-timetable.md C5):
 * <ul>
 *   <li>(a) a classroom's class-teacher's subject (the first configured subject that teacher is
 *       listed as teaching for that class) is placed in period 1 of every day it still has periods
 *       left to schedule. If period 1 can't carry it (teacher double-booked that day/slot, or the
 *       per-class daily cap already reached), period 1 is reported unfilled rather than handed to a
 *       different subject - the rule is "that subject or nothing," not "whoever fits.";</li>
 *   <li>(b) a teacher is never double-booked at the same day+slot across different classrooms -
 *       enforced for the whole run via {@code plannedTeacherSlots}, seeded from whatever that
 *       teacher already has in the database (manually-created periods included);</li>
 *   <li>(c) a teacher teaching one subject to one class/section is capped at 2 NEW periods/day for
 *       that class/section - soft: the cap only ever prevents exceeding 2, it never forces exactly
 *       2, and pre-existing (already in the database) periods for that teacher/class/day are not
 *       counted against it (out of scope for a generator run - see class-level limits below);</li>
 *   <li>(d) a reasonable total daily-load cap per teacher is automatic: a teacher can occupy at
 *       most one slot per day+time, and slots are bounded by the computed day template, so no
 *       explicit extra cap is needed beyond (b).</li>
 * </ul>
 *
 * <p><b>Known limits</b> (documented, not hidden): the soft per-class daily cap (c) and the
 * load-balancing tie-break only account for periods generated in THIS run, not whatever a
 * classroom/teacher already has from manual CRUD or an earlier generator run - a slot already
 * occupied in the database is simply skipped (treated as already filled, never reported unfilled
 * and never overwritten). It is a single greedy pass with no backtracking, so a locally reasonable
 * early choice is never undone later in the same run even if a smarter global arrangement exists -
 * acceptable per the plan ("a constraint-satisfaction/greedy problem, not an exact optimizer").
 */
@Service
@Transactional(readOnly = true)
public class TimetableGeneratorService {

    /** A school's classroom count is bounded; this only guards against abuse, not a real limit. */
    private static final int MAX_CLASSROOMS = 1000;
    private static final List<Integer> DEFAULT_WORKING_DAYS = List.of(1, 2, 3, 4, 5, 6);
    private static final int SOFT_DAILY_CAP_PER_CLASS = 2;

    private final ClassroomRepository classroomRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final TimetableService timetableService;

    public TimetableGeneratorService(ClassroomRepository classroomRepository, TimetablePeriodRepository periodRepository,
                                     TeacherProfileRepository teacherProfileRepository, UserAccountRepository userAccountRepository,
                                     TimetableService timetableService) {
        this.classroomRepository = classroomRepository;
        this.periodRepository = periodRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.timetableService = timetableService;
    }

    /** Dry run: computes the exact same plan {@link #generate} would, writes nothing. */
    public TimetableGenerationResponse preview(Long schoolId, GenerateTimetableRequest request) {
        return run(schoolId, request, false);
    }

    /** Writes every successfully-planned slot as a real {@code TimetablePeriod} via {@code TimetableService.create} - the
     * SAME path Phase 2's manual period CRUD uses, so the teacher's "my periods" view and the admin's period list need no
     * separate code to pick up generated rows. */
    @Transactional
    public TimetableGenerationResponse generate(Long schoolId, GenerateTimetableRequest request) {
        return run(schoolId, request, true);
    }

    // ------------------------------------------------------------------ the algorithm

    private TimetableGenerationResponse run(Long schoolId, GenerateTimetableRequest request, boolean write) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (!request.lastPeriodEnd().isAfter(request.firstPeriodStart())) {
            throw new IllegalArgumentException("lastPeriodEnd must be after firstPeriodStart");
        }
        List<Integer> workingDays = request.workingDays() == null || request.workingDays().isEmpty()
                ? DEFAULT_WORKING_DAYS : request.workingDays().stream().distinct().sorted().toList();

        for (TeacherInput t : request.teachers()) {
            requireTeacherInSchool(schoolId, t.teacherId());
        }
        Map<Long, String> teacherNames = new HashMap<>();
        for (TeacherProfile p : teacherProfileRepository.findByUserIdIn(request.teachers().stream().map(TeacherInput::teacherId).toList())) {
            teacherNames.put(p.getUserId(), p.fullName());
        }
        // subject (lower-cased) -> teachers willing to teach it.
        Map<String, List<Long>> subjectTeachers = new HashMap<>();
        for (TeacherInput t : request.teachers()) {
            for (String subject : t.subjects()) {
                subjectTeachers.computeIfAbsent(subject.toLowerCase(), k -> new ArrayList<>()).add(t.teacherId());
            }
        }

        // className -> subject -> periodsPerWeek (ordered: first-listed subject for a class is the class-teacher-subject candidate).
        Map<String, Map<String, Integer>> loadsByClass = new LinkedHashMap<>();
        for (SubjectLoadInput s : request.subjectLoads()) {
            Map<String, Integer> bySubject = loadsByClass.computeIfAbsent(s.className(), k -> new LinkedHashMap<>());
            if (bySubject.put(s.subject(), s.periodsPerWeek()) != null) {
                throw new IllegalArgumentException("Duplicate subject load for class=" + s.className() + " subject=" + s.subject());
            }
        }

        List<Classroom> allClassrooms = classroomRepository.findBySchoolId(schoolId, PageRequest.of(0, MAX_CLASSROOMS));
        List<Classroom> targets = new ArrayList<>();
        for (String className : request.classNames()) {
            List<Classroom> sections = allClassrooms.stream().filter(c -> c.getName().equalsIgnoreCase(className)).toList();
            if (sections.isEmpty()) {
                throw new IllegalArgumentException("Unknown class (no sections found): " + className);
            }
            targets.addAll(sections);
        }

        List<Slot> daySlots = computeDaySlots(request.firstPeriodStart(), request.lastPeriodEnd(), request.periodLengthMinutes(),
                request.breaks());
        if (daySlots.isEmpty()) {
            throw new IllegalArgumentException("No period slots fit between firstPeriodStart and lastPeriodEnd with that period length");
        }

        // Seed "already busy" from the database: a teacher's existing periods (any classroom) + a classroom's existing periods.
        Map<Long, Map<Integer, List<Range>>> existingTeacherRanges = new HashMap<>();
        for (TeacherInput t : request.teachers()) {
            Map<Integer, List<Range>> byDay = new HashMap<>();
            for (int day : workingDays) {
                byDay.put(day, toRanges(periodRepository.findByTeacherIdAndDayOfWeek(t.teacherId(), day)));
            }
            existingTeacherRanges.put(t.teacherId(), byDay);
        }
        Map<Long, Map<Integer, List<Range>>> existingClassroomRanges = new HashMap<>();
        for (Classroom c : targets) {
            Map<Integer, List<Range>> byDay = new HashMap<>();
            for (int day : workingDays) {
                byDay.put(day, toRanges(periodRepository.findByClassroomIdAndDayOfWeek(c.getId(), day)));
            }
            existingClassroomRanges.put(c.getId(), byDay);
        }

        // Mutable state accumulated across the whole run.
        Map<Long, Map<Integer, List<Range>>> plannedTeacherRanges = new HashMap<>();
        Map<String, Integer> perClassDailyCount = new HashMap<>(); // key: teacherId:classroomId:day
        Map<String, Integer> teacherDailyLoad = new HashMap<>(); // key: teacherId:day

        List<GeneratedPeriod> generated = new ArrayList<>();
        List<UnfilledSlot> unfilled = new ArrayList<>();

        for (Classroom classroom : targets) {
            Map<String, Integer> remaining = new LinkedHashMap<>(loadsByClass.getOrDefault(classroom.getName(), Map.of()));
            int totalWeekly = remaining.values().stream().mapToInt(Integer::intValue).sum();
            String classTeacherSubject = null;
            if (classroom.getClassTeacherId() != null) {
                for (String subject : remaining.keySet()) {
                    List<Long> eligible = subjectTeachers.getOrDefault(subject.toLowerCase(), List.of());
                    if (eligible.contains(classroom.getClassTeacherId())) {
                        classTeacherSubject = subject;
                        break;
                    }
                }
            }

            for (int day : workingDays) {
                for (int slotIndex = 0; slotIndex < daySlots.size(); slotIndex++) {
                    Slot slot = daySlots.get(slotIndex);
                    List<Range> existingHere = existingClassroomRanges.get(classroom.getId()).get(day);
                    if (overlapsAny(slot, existingHere)) {
                        continue; // already filled by a pre-existing (manually created) period - leave it alone.
                    }

                    if (slotIndex == 0 && classTeacherSubject != null && remaining.getOrDefault(classTeacherSubject, 0) > 0) {
                        Long teacherId = classroom.getClassTeacherId();
                        if (canAssign(teacherId, classroom.getId(), day, slot, existingTeacherRanges, plannedTeacherRanges, perClassDailyCount)) {
                            assign(classroom, day, slot, teacherId, teacherNames.get(teacherId), classTeacherSubject, true,
                                    plannedTeacherRanges, perClassDailyCount, teacherDailyLoad, remaining, generated, write, schoolId, unfilled);
                        } else {
                            unfilled.add(new UnfilledSlot(classroom.getId(), classroom.displayName(), day, slot.start(), slot.end(),
                                    classTeacherSubject, "class teacher's subject could not be placed in period 1 (teacher clash or daily cap)"));
                        }
                        continue;
                    }

                    List<String> candidates = remaining.entrySet().stream()
                            .filter(e -> e.getValue() > 0)
                            .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(e -> -e.getValue()).thenComparing(Map.Entry::getKey))
                            .map(Map.Entry::getKey)
                            .toList();
                    boolean assigned = false;
                    for (String subject : candidates) {
                        Long pick = subjectTeachers.getOrDefault(subject.toLowerCase(), List.of()).stream()
                                .filter(tid -> canAssign(tid, classroom.getId(), day, slot, existingTeacherRanges, plannedTeacherRanges, perClassDailyCount))
                                .min(Comparator.comparingInt((Long tid) -> teacherDailyLoad.getOrDefault(tid + ":" + day, 0)).thenComparing(t -> t))
                                .orElse(null);
                        if (pick != null) {
                            assign(classroom, day, slot, pick, teacherNames.get(pick), subject, false,
                                    plannedTeacherRanges, perClassDailyCount, teacherDailyLoad, remaining, generated, write, schoolId, unfilled);
                            assigned = true;
                            break;
                        }
                    }
                    if (!assigned && !candidates.isEmpty()) {
                        unfilled.add(new UnfilledSlot(classroom.getId(), classroom.displayName(), day, slot.start(), slot.end(),
                                null, "no eligible teacher free for any subject with remaining demand at this time"));
                    }
                }
            }

            for (Map.Entry<String, Integer> leftover : remaining.entrySet()) {
                if (leftover.getValue() > 0) {
                    unfilled.add(new UnfilledSlot(classroom.getId(), classroom.displayName(), null, null, null, leftover.getKey(),
                            leftover.getValue() + " of " + totalWeekly + " weekly periods for " + leftover.getKey()
                                    + " could not be scheduled this week - not enough free slots"));
                }
            }
        }

        return new TimetableGenerationResponse(generated, unfilled, write);
    }

    private void assign(Classroom classroom, int day, Slot slot, Long teacherId, String teacherName, String subject,
                        boolean classTeacherPeriod, Map<Long, Map<Integer, List<Range>>> plannedTeacherRanges,
                        Map<String, Integer> perClassDailyCount, Map<String, Integer> teacherDailyLoad,
                        Map<String, Integer> remaining, List<GeneratedPeriod> generated, boolean write, Long schoolId,
                        List<UnfilledSlot> unfilled) {
        if (write) {
            try {
                timetableService.create(schoolId, classroom.getId(), new PeriodRequest(teacherId, subject, day, slot.start(), slot.end()));
            } catch (ConflictException | ResourceNotFoundException | IllegalArgumentException e) {
                unfilled.add(new UnfilledSlot(classroom.getId(), classroom.displayName(), day, slot.start(), slot.end(), subject,
                        "write failed: " + e.getMessage()));
                return;
            }
        }
        plannedTeacherRanges.computeIfAbsent(teacherId, k -> new HashMap<>())
                .computeIfAbsent(day, k -> new ArrayList<>())
                .add(new Range(slot.start(), slot.end()));
        perClassDailyCount.merge(teacherId + ":" + classroom.getId() + ":" + day, 1, Integer::sum);
        teacherDailyLoad.merge(teacherId + ":" + day, 1, Integer::sum);
        remaining.merge(subject, -1, Integer::sum);
        generated.add(new GeneratedPeriod(classroom.getId(), classroom.displayName(), day, slot.start(), slot.end(), teacherId,
                teacherName, subject, classTeacherPeriod));
    }

    private static boolean canAssign(Long teacherId, Long classroomId, int day, Slot slot,
                                     Map<Long, Map<Integer, List<Range>>> existingTeacherRanges,
                                     Map<Long, Map<Integer, List<Range>>> plannedTeacherRanges,
                                     Map<String, Integer> perClassDailyCount) {
        List<Range> existing = existingTeacherRanges.getOrDefault(teacherId, Map.of()).getOrDefault(day, List.of());
        if (overlapsAny(slot, existing)) {
            return false;
        }
        List<Range> planned = plannedTeacherRanges.getOrDefault(teacherId, Map.of()).getOrDefault(day, List.of());
        if (overlapsAny(slot, planned)) {
            return false;
        }
        int soFar = perClassDailyCount.getOrDefault(teacherId + ":" + classroomId + ":" + day, 0);
        return soFar < SOFT_DAILY_CAP_PER_CLASS;
    }

    private static boolean overlapsAny(Slot slot, List<Range> ranges) {
        for (Range r : ranges) {
            if (slot.start().isBefore(r.end()) && slot.end().isAfter(r.start())) {
                return true;
            }
        }
        return false;
    }

    private static List<Range> toRanges(List<TimetablePeriod> periods) {
        List<Range> ranges = new ArrayList<>(periods.size());
        for (TimetablePeriod p : periods) {
            ranges.add(new Range(p.getStartTime(), p.getEndTime()));
        }
        return ranges;
    }

    /** The day's period slots, in order, computed once from first/last time + period length, with breaks carved out. */
    private static List<Slot> computeDaySlots(LocalTime first, LocalTime last, int periodLengthMinutes, List<BreakInput> breaks) {
        List<Slot> slots = new ArrayList<>();
        List<BreakInput> sorted = breaks == null ? List.of() : breaks.stream().sorted(Comparator.comparing(BreakInput::start)).toList();
        LocalTime cursor = first;
        int guard = 0;
        while (!cursor.plusMinutes(periodLengthMinutes).isAfter(last) && guard++ < 200) {
            LocalTime slotEnd = cursor.plusMinutes(periodLengthMinutes);
            BreakInput hit = null;
            for (BreakInput b : sorted) {
                LocalTime breakEnd = b.start().plusMinutes(b.durationMinutes());
                if (cursor.isBefore(breakEnd) && slotEnd.isAfter(b.start())) {
                    hit = b;
                    break;
                }
            }
            if (hit != null) {
                LocalTime afterBreak = hit.start().plusMinutes(hit.durationMinutes());
                if (!afterBreak.isAfter(cursor)) {
                    break; // malformed break (ends before/at cursor) - stop rather than loop forever.
                }
                cursor = afterBreak;
                continue;
            }
            slots.add(new Slot(cursor, slotEnd));
            cursor = slotEnd;
        }
        return slots;
    }

    private void requireTeacherInSchool(Long schoolId, Long teacherId) {
        UserAccount user = userAccountRepository.findById(teacherId).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER || !Objects.equals(user.getSchoolId(), schoolId)) {
            throw ResourceNotFoundException.of("Teacher", teacherId);
        }
    }

    private record Slot(LocalTime start, LocalTime end) {
    }

    private record Range(LocalTime start, LocalTime end) {
    }
}
