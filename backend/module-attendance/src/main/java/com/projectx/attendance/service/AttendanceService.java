package com.projectx.attendance.service;

import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.domain.Subscription;
import com.projectx.attendance.dto.AttendanceDtos;
import com.projectx.attendance.dto.AttendanceDtos.AttendanceSummary;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceRequest;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceResult;
import com.projectx.attendance.dto.AttendanceDtos.BulkEntry;
import com.projectx.attendance.dto.AttendanceDtos.SheetRow;
import com.projectx.attendance.repo.ClassroomRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The attendance sheet. All reads and the bulk write are set-based SQL (JdbcTemplate) against the
 * month-partitioned {@code school_attendance} table: one statement per request regardless of class
 * size, no entity-per-row hydration, partition pruning on attendance_date.
 *
 * <p>Bulk write = idempotent upsert on UNIQUE (student_id, attendance_date), last-write-wins by
 * design (a teacher correcting a mark is the normal case, not a conflict); rows already holding the
 * requested status are not rewritten at all. Not class-level @Transactional: the attendance upsert
 * commits first, and the dependent Arrival confirmations are applied afterwards by ArrivalService in
 * their own retrying transaction - a contended arrival can never roll back a teacher's attendance
 * sheet, and re-sending the sheet repairs any arrival that missed its update.
 */
@Service
public class AttendanceService {

    private static final int ARRIVAL_RANK_ALERT = 3;
    private static final int ARRIVAL_RANK_PENDING = 2;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ClassroomRepository classroomRepository;
    private final TeacherScope teacherScope;
    private final ArrivalService arrivalService;
    private final SummaryCache summaryCache;
    private final ApplicationEventPublisher eventPublisher;
    private final int teacherBackdateDays;
    private final int adminBackdateDays;

    public AttendanceService(JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                             ClassroomRepository classroomRepository, TeacherScope teacherScope, ArrivalService arrivalService,
                             SummaryCache summaryCache, ApplicationEventPublisher eventPublisher,
                             @Value("${projectx.attendance.teacher-backdate-days:31}") int teacherBackdateDays,
                             @Value("${projectx.attendance.admin-backdate-days:400}") int adminBackdateDays) {
        this.teacherBackdateDays = teacherBackdateDays;
        this.adminBackdateDays = adminBackdateDays;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
        this.classroomRepository = classroomRepository;
        this.teacherScope = teacherScope;
        this.arrivalService = arrivalService;
        this.summaryCache = summaryCache;
        this.eventPublisher = eventPublisher;
    }

    // ------------------------------------------------------------------ sheet

    public List<SheetRow> sheet(Long schoolId, LocalDate date, Long classroomId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (TeacherScope.isTeacher() && classroomId == null) {
            throw new IllegalArgumentException("classroomId is required");
        }
        if (classroomId != null) {
            requireClassroom(schoolId, classroomId);
            // Marking or viewing the register is the class teacher's job; a lecturer or unrelated teacher gets a 403.
            teacherScope.requireClassTeacher(schoolId, classroomId);
        }
        int cap = AttendanceDtos.MAX_BULK_ENTRIES;
        StringBuilder sql = new StringBuilder("""
                SELECT s.id, s.first_name, s.last_name, s.roll_no, s.child_id, a.status,
                       (SELECT max(CASE av.outcome WHEN 'ALERT' THEN %d WHEN 'PENDING' THEN %d ELSE 1 END)
                          FROM arrival_verifications av
                         WHERE av.student_id = s.id AND av.arrival_date = ?) AS arrival_rank
                  FROM students s
                  LEFT JOIN school_attendance a ON a.student_id = s.id AND a.attendance_date = ?
                 WHERE s.school_id = ?
                """.formatted(ARRIVAL_RANK_ALERT, ARRIVAL_RANK_PENDING));
        List<Object> args = new ArrayList<>(List.of(Date.valueOf(date), Date.valueOf(date), schoolId));
        if (classroomId != null) {
            sql.append(" AND s.classroom_id = ?\n");
            args.add(classroomId);
        }
        sql.append(" ORDER BY s.roll_no NULLS LAST, s.last_name, s.first_name, s.id LIMIT ").append(cap + 1);

        List<SheetRow> rows = jdbc.query(sql.toString(), (rs, i) -> {
            String status = rs.getString("status");
            int rank = rs.getInt("arrival_rank");
            boolean hasArrival = !rs.wasNull();
            return new SheetRow(rs.getLong("id"), rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("roll_no"),
                    rs.getObject("child_id") != null ? Subscription.SUBSCRIBED : Subscription.NON_SUBSCRIBED,
                    status == null ? null : AttendanceStatus.valueOf(status),
                    !hasArrival ? null : rank >= ARRIVAL_RANK_ALERT ? ArrivalOutcome.ALERT
                            : rank == ARRIVAL_RANK_PENDING ? ArrivalOutcome.PENDING : ArrivalOutcome.SAFE);
        }, args.toArray());
        if (rows.size() > cap) {
            throw new IllegalArgumentException("More than " + cap + " students match - pass a classroomId to narrow the sheet");
        }
        return rows;
    }

    // ------------------------------------------------------------------ bulk write

    public BulkAttendanceResult bulk(Long schoolId, BulkAttendanceRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        LocalDate date = request.date();
        if (date.isAfter(SchoolTime.today())) {
            throw new IllegalArgumentException("Cannot mark attendance for a future date");
        }
        // Back-dating window: teachers may only correct the recent past; admins have a long sanity floor (every
        // admin edit is audited as ATTENDANCE_EDITED). The floor also keeps rows out of the DEFAULT partition.
        CurrentUser actor = CurrentUserContext.get();
        int windowDays = actor.role() == Role.TEACHER ? teacherBackdateDays : adminBackdateDays;
        if (date.isBefore(SchoolTime.today().minusDays(windowDays))) {
            throw new IllegalArgumentException("Attendance can only be marked or corrected for the last " + windowDays
                    + " days by your role - ask a school admin for older dates");
        }
        requireClassroom(schoolId, request.classroomId());
        teacherScope.requireClassTeacher(schoolId, request.classroomId());

        Map<Long, AttendanceStatus> byStudent = new LinkedHashMap<>();
        for (BulkEntry entry : request.entries()) {
            if (byStudent.put(entry.studentId(), entry.status()) != null) {
                throw new IllegalArgumentException("Student " + entry.studentId() + " appears more than once");
            }
        }
        CurrentUser user = CurrentUserContext.get();
        // C3: "once a day's attendance is submitted" = once that day is over - a TEACHER can still freely correct
        // TODAY's register all day (unchanged from Phase 2: mark, re-mark, flip back, as many times as needed), but a
        // PAST day's stored status can no longer be changed directly, only via a correction request. Adding a row for
        // a student not yet marked on a past day (no existing row) is still a plain insert, not an edit, so that keeps
        // working too - this only blocks CHANGING a status that's already stored for a day that's already over.
        // SCHOOL_ADMIN/PLATFORM_ADMIN are exempt (direct edits unchanged for them, any date).
        if (actor.role() == Role.TEACHER && date.isBefore(SchoolTime.today())) {
            requireNoDirectTeacherEdit(request.classroomId(), date, byStudent);
        }
        // The audit event is published INSIDE the transaction: the audit listener is AFTER_COMMIT, which only fires
        // for events published while a transaction is active (outside one it is silently dropped). It therefore
        // also never records an edit whose write rolled back.
        Counts counts = tx.execute(status -> {
            Counts c = upsert(schoolId, request.classroomId(), date, byStudent, user.userId());
            if (c.inserted + c.updated > 0) {
                int unchangedRows = byStudent.size() - c.inserted - c.updated;
                String action = c.updated > 0 ? "ATTENDANCE_EDITED" : "ATTENDANCE_MARKED";
                eventPublisher.publishEvent(AuditEvent.of(action, "ATTENDANCE", request.classroomId(), schoolId,
                        "date=" + date + " classroom=" + request.classroomId() + " inserted=" + c.inserted
                                + " changed=" + c.updated + " unchanged=" + unchangedRows
                                + (c.changedIds.isEmpty() ? "" : " editedStudents=" + c.changedIds)));
            }
            return c;
        });

        // After the attendance commit: bring Arrivals in line (own retrying tx), then drop the cached summary.
        arrivalService.syncSchoolConfirmations(byStudent, date);
        summaryCache.evict(schoolId, date);

        int unchanged = byStudent.size() - counts.inserted - counts.updated;
        return new BulkAttendanceResult(date, request.classroomId(), byStudent.size(), counts.inserted, counts.updated, unchanged);
    }

    /** 403 if any entry would change an already-stored status for a TEACHER (see {@link #bulk}'s C3 lock). */
    private void requireNoDirectTeacherEdit(Long classroomId, LocalDate date, Map<Long, AttendanceStatus> byStudent) {
        String ids = "{" + byStudent.keySet().stream().map(String::valueOf).collect(Collectors.joining(",")) + "}";
        List<Long> locked = new ArrayList<>();
        jdbc.query("SELECT student_id, status FROM school_attendance WHERE classroom_id = ? AND attendance_date = ? "
                        + "AND student_id = ANY(?::bigint[])",
                rs -> {
                    Long studentId = rs.getLong(1);
                    AttendanceStatus stored = AttendanceStatus.valueOf(rs.getString(2));
                    if (stored != byStudent.get(studentId)) {
                        locked.add(studentId);
                    }
                }, classroomId, Date.valueOf(date), ids);
        if (!locked.isEmpty()) {
            throw new ForbiddenOperationException("This day's attendance is already submitted for student(s) " + locked
                    + " - submit a correction request instead of editing directly");
        }
    }

    /**
     * Package-visible single-row upsert for {@link AttendanceCorrectionService#decide} to call FROM WITHIN its own
     * transaction (the correction request's decision and the attendance change must commit together, unlike
     * {@link #bulk}, which deliberately commits the attendance write before the arrival resync). No audit event or
     * arrival resync here - the caller owns both (it needs the correction-request row in the same audit detail / the
     * same post-commit resync call).
     */
    Counts upsertForCorrection(Long schoolId, Long classroomId, LocalDate date, Long studentId, AttendanceStatus status, Long actorUserId) {
        return upsert(schoolId, classroomId, date, Map.of(studentId, status), actorUserId);
    }

    record Counts(int inserted, int updated, List<Long> changedIds) {
    }

    /** Validates ownership then performs the single batched upsert. Runs inside the caller's transaction. */
    private Counts upsert(Long schoolId, Long classroomId, LocalDate date, Map<Long, AttendanceStatus> byStudent, Long userId) {
        String ids = "{" + byStudent.keySet().stream().map(String::valueOf).collect(Collectors.joining(",")) + "}";
        String statuses = "{" + byStudent.values().stream().map(Enum::name).collect(Collectors.joining(",")) + "}";

        // Ownership check: every id must be a student of THIS school AND this classroom. Cross-tenant ids fail the whole request.
        Integer valid = jdbc.queryForObject(
                "SELECT count(*) FROM students WHERE id = ANY(?::bigint[]) AND school_id = ? AND classroom_id = ?",
                Integer.class, ids, schoolId, classroomId);
        if (valid == null || valid != byStudent.size()) {
            throw new IllegalArgumentException((byStudent.size() - (valid == null ? 0 : valid))
                    + " of the entries are not students of this classroom");
        }

        // One statement for the whole sheet. Rows already at the requested status are skipped (WHERE ... IS DISTINCT FROM),
        // so replays/duplicates cause no write amplification; RETURNING tells inserts (xmax = 0) from edits.
        List<Object[]> returned = jdbc.query("""
                INSERT INTO school_attendance AS a (school_id, student_id, classroom_id, attendance_date, status, marked_by_user_id, marked_at)
                SELECT ?, t.student_id, ?, ?, t.status, ?, now()
                  FROM unnest(?::bigint[], ?::text[]) AS t(student_id, status)
                ON CONFLICT (student_id, attendance_date) DO UPDATE
                   SET status = EXCLUDED.status, classroom_id = EXCLUDED.classroom_id,
                       marked_by_user_id = EXCLUDED.marked_by_user_id, marked_at = EXCLUDED.marked_at,
                       version = a.version + 1
                 WHERE a.status IS DISTINCT FROM EXCLUDED.status
                RETURNING a.student_id, (a.version = 0) AS inserted
                """, (rs, i) -> new Object[] {rs.getLong(1), rs.getBoolean(2)},
                schoolId, classroomId, Date.valueOf(date), userId, ids, statuses);
        int inserted = 0;
        List<Long> edited = new ArrayList<>();
        for (Object[] row : returned) {
            if ((Boolean) row[1]) {
                inserted++;
            } else if (edited.size() < 20) {
                edited.add((Long) row[0]);
            }
        }
        return new Counts(inserted, returned.size() - inserted, edited);
    }

    // ------------------------------------------------------------------ summary

    public AttendanceSummary summary(Long schoolId, LocalDate date) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        LocalDate day = date != null ? date : SchoolTime.today();
        var cached = summaryCache.get(schoolId, day);
        if (cached.isPresent()) {
            return cached.get();
        }
        // Three indexed aggregates in one round trip - never a list fetch.
        AttendanceSummary summary = jdbc.queryForObject("""
                SELECT (SELECT count(*) FROM students WHERE school_id = ?) AS total,
                       (SELECT count(child_id) FROM students WHERE school_id = ?) AS subscribed,
                       (SELECT count(*) FROM school_attendance WHERE school_id = ? AND attendance_date = ? AND status = 'PRESENT') AS present,
                       (SELECT count(*) FROM school_attendance WHERE school_id = ? AND attendance_date = ? AND status = 'ABSENT') AS absent,
                       (SELECT count(*) FROM school_attendance WHERE school_id = ? AND attendance_date = ? AND status = 'LATE') AS late,
                       (SELECT count(*) FROM arrival_verifications WHERE school_id = ? AND arrival_date = ? AND outcome = 'ALERT') AS mismatches
                """, (rs, i) -> {
                    long total = rs.getLong("total");
                    long subscribed = rs.getLong("subscribed");
                    long present = rs.getLong("present");
                    long absent = rs.getLong("absent");
                    long late = rs.getLong("late");
                    return new AttendanceSummary(total, present, absent, late, Math.max(0, total - present - absent - late),
                            subscribed, total - subscribed, rs.getLong("mismatches"));
                }, schoolId, schoolId, schoolId, Date.valueOf(day), schoolId, Date.valueOf(day), schoolId, Date.valueOf(day),
                schoolId, Date.valueOf(day));
        summaryCache.put(schoolId, day, summary);
        return summary;
    }

    private void requireClassroom(Long schoolId, Long classroomId) {
        if (classroomRepository.findByIdAndSchoolId(classroomId, schoolId).isEmpty()) {
            throw ResourceNotFoundException.of("Classroom", classroomId);
        }
    }
}
