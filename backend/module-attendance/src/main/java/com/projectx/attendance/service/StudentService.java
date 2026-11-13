package com.projectx.attendance.service;

import com.projectx.attendance.domain.Classroom;
import com.projectx.attendance.domain.Student;
import com.projectx.attendance.domain.Subscription;
import com.projectx.attendance.dto.StudentDtos.DirectoryRow;
import com.projectx.attendance.dto.StudentDtos.ImportResult;
import com.projectx.attendance.dto.StudentDtos.StudentRequest;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.attendance.dto.StudentDtos.StudentSearchResult;
import com.projectx.attendance.repo.ClassroomRepository;
import com.projectx.attendance.repo.StudentRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.common.web.PageResponse;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_IMPORT_ROWS = 500;

    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TeacherScope teacherScope;
    private final ConsentService consentService;

    public StudentService(StudentRepository studentRepository, ClassroomRepository classroomRepository,
                          ChildRepository childRepository, ParentProfileRepository parentProfileRepository,
                          ApplicationEventPublisher eventPublisher, TeacherScope teacherScope, ConsentService consentService) {
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.eventPublisher = eventPublisher;
        this.teacherScope = teacherScope;
        this.consentService = consentService;
    }

    /**
     * Admins see the whole roster. A TEACHER sees only students of classes they are class teacher of (full details) or lecture
     * in (guardian contact and consent fields removed); asking for a class they have no relation to is a 403, and with no
     * classroomId they get the union of their own classes.
     */
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> list(Long schoolId, Long classroomId, Subscription subscription, String q,
                                              Integer page, Integer size) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        boolean teacher = TeacherScope.isTeacher();
        Set<Long> teacherClasses = Set.of();
        Set<Long> classTeacherOf = Set.of();
        if (teacher) {
            if (classroomId != null) {
                if (teacherScope.relation(schoolId, classroomId) == TeacherScope.Relation.NONE) {
                    throw new ForbiddenOperationException("You do not teach that class");
                }
            }
            teacherClasses = teacherScope.relatedClassroomIds(schoolId);
            classTeacherOf = teacherScope.classTeacherClassroomId().map(Set::of).orElse(Set.of());
        }
        final Set<Long> allowedClasses = teacherClasses;
        Specification<Student> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("schoolId"), schoolId));
            if (classroomId != null) {
                predicates.add(cb.equal(root.get("classroomId"), classroomId));
            }
            if (teacher) {
                predicates.add(allowedClasses.isEmpty() ? cb.disjunction() : root.get("classroomId").in(allowedClasses));
            }
            if (subscription == Subscription.SUBSCRIBED) {
                predicates.add(cb.isNotNull(root.get("childId")));
            } else if (subscription == Subscription.NON_SUBSCRIBED) {
                predicates.add(cb.isNull(root.get("childId")));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("rollNo")), pattern, '\\'),
                        cb.like(root.get("guardianPhone"), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), clampSize(size),
                Sort.by("lastName", "firstName", "id"));
        var result = studentRepository.findAll(spec, pageable);
        Map<Long, ConsentService.ConsentInfo> consent = consentService.statusFor(result.getContent());
        final Set<Long> fullDetailClasses = classTeacherOf;
        return PageResponse.from(result.map(s -> {
            StudentResponse r = toResponse(s, consent.get(s.getId()));
            // A lecturer of this class may record marks, but does not get to see who to call.
            return teacher && !fullDetailClasses.contains(s.getClassroomId()) ? r.withoutGuardianContact() : r;
        }));
    }

    /**
     * C2: the school-admin "Students & Guardians" directory - admin-only (unlike {@link #list}, there is no teacher
     * scoping branch here), name search + classroom filter, with each row's classroom name/section resolved in one
     * batch lookup instead of per-row.
     */
    @Transactional(readOnly = true)
    public PageResponse<DirectoryRow> directory(Long schoolId, Long classroomId, String q, Integer page, Integer size) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Specification<Student> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("schoolId"), schoolId));
            if (classroomId != null) {
                predicates.add(cb.equal(root.get("classroomId"), classroomId));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("lastName")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), clampSize(size),
                Sort.by("lastName", "firstName", "id"));
        var result = studentRepository.findAll(spec, pageable);

        Set<Long> classroomIds = new HashSet<>();
        for (Student s : result.getContent()) {
            if (s.getClassroomId() != null) {
                classroomIds.add(s.getClassroomId());
            }
        }
        Map<Long, Classroom> classrooms = new HashMap<>();
        if (!classroomIds.isEmpty()) {
            for (Classroom c : classroomRepository.findAllById(classroomIds)) {
                classrooms.put(c.getId(), c);
            }
        }
        return PageResponse.from(result.map(s -> {
            Classroom c = s.getClassroomId() == null ? null : classrooms.get(s.getClassroomId());
            return new DirectoryRow(s.getId(), (s.getFirstName() + " " + s.getLastName()).trim(), s.getClassroomId(),
                    c == null ? null : c.getName(), c == null ? null : c.getSection(), s.getGuardianName(),
                    s.getGuardianPhone(), s.getGuardianEmail(),
                    s.isSubscribed() ? Subscription.SUBSCRIBED : Subscription.NON_SUBSCRIBED);
        }));
    }

    /**
     * E4: student picker for a parent raising a grievance about another student - scoped to a school their OWN child
     * attends (a PARENT token carries no schoolId claim, so this is checked via guardianship, not
     * {@code CurrentUserContext.requireSchoolAccess}), and intentionally minimal (name + class + section only, no
     * guardian contact or other PII - see {@link com.projectx.attendance.dto.StudentDtos.StudentSearchResult}).
     */
    @Transactional(readOnly = true)
    public List<StudentSearchResult> searchForParent(Long schoolId, String q) {
        CurrentUser user = CurrentUserContext.get();
        if (user.role() != Role.PARENT) {
            throw new ForbiddenOperationException("Parent-only endpoint");
        }
        ParentProfile parent = parentProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for current user"));
        boolean member = Objects.equals(parent.getInvitedBySchoolId(), schoolId)
                || childRepository.findByGuardianParentId(parent.getId()).stream().anyMatch(c -> Objects.equals(c.getSchoolId(), schoolId));
        if (!member) {
            throw new ForbiddenOperationException("Not authorized for this school");
        }
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        String pattern = "%" + escapeLike(q.trim().toLowerCase()) + "%";
        Specification<Student> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("schoolId"), schoolId),
                cb.or(cb.like(cb.lower(root.get("firstName")), pattern, '\\'), cb.like(cb.lower(root.get("lastName")), pattern, '\\')));
        List<Student> matches = studentRepository.findAll(spec, PageRequest.of(0, 20, Sort.by("lastName", "firstName", "id"))).getContent();
        Set<Long> classroomIds = new HashSet<>();
        for (Student s : matches) {
            if (s.getClassroomId() != null) {
                classroomIds.add(s.getClassroomId());
            }
        }
        Map<Long, Classroom> classrooms = new HashMap<>();
        for (Classroom c : classroomRepository.findAllById(classroomIds)) {
            classrooms.put(c.getId(), c);
        }
        return matches.stream().map(s -> {
            Classroom c = s.getClassroomId() == null ? null : classrooms.get(s.getClassroomId());
            return new StudentSearchResult(s.getId(), (s.getFirstName() + " " + s.getLastName()).trim(),
                    c == null ? null : c.getName(), c == null ? null : c.getSection());
        }).toList();
    }

    public StudentResponse create(Long schoolId, StudentRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        requireClassroomInSchool(schoolId, request.classroomId());
        Student student = new Student(schoolId);
        apply(student, request, null, "ADMIN_EDIT");
        try {
            student = studentRepository.saveAndFlush(student);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("That roll number is already used in this classroom");
        }
        return toResponse(student);
    }

    /** CLASS_TEACHER (or admin): everything. LECTURER: the student without guardian contact. Anyone else: 403. */
    @Transactional(readOnly = true)
    public StudentResponse get(Long schoolId, Long studentId) {
        Student student = requireStudent(schoolId, studentId);
        TeacherScope.Relation relation = teacherScope.requireAnyRelation(student);
        StudentResponse response = toResponse(student);
        return relation == TeacherScope.Relation.LECTURER ? response.withoutGuardianContact() : response;
    }

    private StudentResponse toResponse(Student student) {
        return toResponse(student, consentService.statusFor(List.of(student)).get(student.getId()));
    }

    private static StudentResponse toResponse(Student student, ConsentService.ConsentInfo info) {
        return info == null ? StudentResponse.from(student) : StudentResponse.from(student, info.status(), info.expiresAt());
    }

    public StudentResponse update(Long schoolId, Long studentId, StudentRequest request) {
        if (request.version() == null) {
            // Last-write-wins on a roster record is how one colleague silently erases another's edit.
            throw new IllegalArgumentException("version is required when updating a student (send the version you loaded)");
        }
        Student student = requireStudent(schoolId, studentId);
        if (!request.version().equals(student.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Student.class, studentId);
        }
        requireClassroomInSchool(schoolId, request.classroomId());
        boolean consentBefore = student.isMarketingConsent();
        apply(student, request, student, "ADMIN_EDIT");
        try {
            student = studentRepository.saveAndFlush(student);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("That roll number is already used in this classroom");
        }
        if (consentBefore != student.isMarketingConsent()) {
            eventPublisher.publishEvent(AuditEvent.of("STUDENT_MARKETING_CONSENT_CHANGED", "STUDENT", studentId, schoolId,
                    "marketingConsent " + consentBefore + " -> " + student.isMarketingConsent()));
        }
        return toResponse(student);
    }

    /**
     * All-or-nothing: validation problems reject the whole batch (400) so a half-imported roster
     * never has to be reconciled by hand. Rows matching an existing student (same classroom + roll
     * number) update it, so re-uploading the same sheet is idempotent. Inserts are JDBC-batched
     * (sequence-generated ids, see students_seq / hibernate.jdbc.batch_size).
     */
    public ImportResult importStudents(Long schoolId, List<StudentRequest> rows) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("No rows to import");
        }
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new IllegalArgumentException("At most " + MAX_IMPORT_ROWS + " rows per import (got " + rows.size() + ")");
        }
        Set<Long> classroomIds = new HashSet<>();
        Set<String> rollNos = new HashSet<>();
        Set<String> seenInPayload = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            StudentRequest row = rows.get(i);
            if (row == null) {
                throw new IllegalArgumentException("Row " + (i + 1) + " is empty");
            }
            if (row.classroomId() != null) {
                classroomIds.add(row.classroomId());
            }
            String roll = normalizeRoll(row.rollNo());
            if (roll != null) {
                rollNos.add(roll);
                if (row.classroomId() != null && !seenInPayload.add(row.classroomId() + "|" + roll)) {
                    throw new IllegalArgumentException("Row " + (i + 1) + ": duplicate roll number " + roll + " in the same classroom");
                }
            }
        }
        for (Long classroomId : classroomIds) {
            requireClassroomInSchool(schoolId, classroomId);
        }

        Map<String, Student> existing = new HashMap<>();
        if (!rollNos.isEmpty()) {
            for (Student s : studentRepository.findBySchoolIdAndRollNoIn(schoolId, rollNos)) {
                if (s.getClassroomId() != null) {
                    existing.put(s.getClassroomId() + "|" + s.getRollNo(), s);
                }
            }
        }

        List<Student> toInsert = new ArrayList<>();
        int updated = 0;
        int consented = 0;
        for (StudentRequest row : rows) {
            String roll = normalizeRoll(row.rollNo());
            Student match = roll != null && row.classroomId() != null ? existing.get(row.classroomId() + "|" + roll) : null;
            if (match != null) {
                apply(match, row, match, "IMPORT");
                updated++;
            } else {
                Student fresh = new Student(schoolId);
                apply(fresh, row, null, "IMPORT");
                toInsert.add(fresh);
            }
            if (Boolean.TRUE.equals(row.marketingConsent())) {
                consented++;
            }
        }
        try {
            studentRepository.saveAll(toInsert);
            studentRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Import conflicts with existing roll numbers - nothing was imported");
        }
        eventPublisher.publishEvent(AuditEvent.of("STUDENT_IMPORT", "STUDENT", null, schoolId,
                "created=" + toInsert.size() + " updated=" + updated + " rowsWithMarketingConsent=" + consented));
        return new ImportResult(toInsert.size(), updated);
    }

    public StudentResponse linkChild(Long schoolId, Long studentId, Long childId) {
        Student student = requireStudent(schoolId, studentId);
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!child.getSchoolId().equals(schoolId)) {
            // Same 404 as a missing child: never confirm to one tenant that another tenant's child id exists.
            throw ResourceNotFoundException.of("Child", childId);
        }
        if (Objects.equals(student.getChildId(), childId)) {
            return toResponse(student);
        }
        if (student.getChildId() != null) {
            throw new ConflictException("This student is already linked to another child - unlink first");
        }
        studentRepository.findByChildId(childId).ifPresent(other -> {
            throw new ConflictException("That child is already linked to another student");
        });
        student.linkChild(childId);
        try {
            student = studentRepository.saveAndFlush(student);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("That child is already linked to another student");
        }
        eventPublisher.publishEvent(AuditEvent.of("STUDENT_CHILD_LINKED", "STUDENT", studentId, schoolId, "childId=" + childId));
        // Back-fill (async, after commit) the studentId + school confirmation of this child's arrivals opened before the link existed.
        eventPublisher.publishEvent(new StudentChildLinkedEvent(studentId, childId));
        return toResponse(student);
    }

    public StudentResponse unlinkChild(Long schoolId, Long studentId) {
        Student student = requireStudent(schoolId, studentId);
        Long previous = student.getChildId();
        if (previous != null) {
            student.unlinkChild();
            student = studentRepository.saveAndFlush(student);
            eventPublisher.publishEvent(AuditEvent.of("STUDENT_CHILD_UNLINKED", "STUDENT", studentId, schoolId, "childId=" + previous));
        }
        return toResponse(student);
    }

    /** Tenant-checked lookup shared with the academic-records services. A student of another school is a 404, not a 403. */
    @Transactional(readOnly = true)
    public Student requireStudent(Long schoolId, Long studentId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));
    }

    private void requireClassroomInSchool(Long schoolId, Long classroomId) {
        if (classroomId != null && classroomRepository.findByIdAndSchoolId(classroomId, schoolId).isEmpty()) {
            throw ResourceNotFoundException.of("Classroom", classroomId);
        }
    }

    /** {@code current} non-null = update: fields the caller left null keep their stored value for consent/lead source (an omitted flag must never silently flip consent). */
    private void apply(Student target, StudentRequest r, Student current, String consentSource) {
        boolean consent = r.marketingConsent() != null ? r.marketingConsent() : current != null && current.isMarketingConsent();
        String leadSource = r.leadSource() != null ? r.leadSource() : current != null ? current.getLeadSource() : null;
        boolean consentBefore = current != null && current.isMarketingConsent();
        target.applyDetails(r.classroomId(), normalizeRoll(r.rollNo()), r.firstName().trim(), r.lastName().trim(), r.dob(),
                blankToNull(r.gender()), r.guardianName().trim(), r.guardianPhone().trim(), blankToNull(r.guardianEmail()),
                consent, leadSource);
        // Provenance: remember when and how the consent flag last changed (a brand-new row with consent=false has none).
        if (consent != consentBefore) {
            target.stampConsent(consentSource);
        }
    }

    private static String normalizeRoll(String roll) {
        return blankToNull(roll);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static int clampSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
