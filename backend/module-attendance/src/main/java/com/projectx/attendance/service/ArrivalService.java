package com.projectx.attendance.service;

import com.projectx.attendance.domain.Arrival;
import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.domain.ParentResponse;
import com.projectx.attendance.domain.SchoolConfirm;
import com.projectx.attendance.domain.Student;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.ArrivalDtos.RespondRequest;
import com.projectx.attendance.repo.ArrivalRepository;
import com.projectx.attendance.repo.StudentRepository;
import com.projectx.common.events.ChildTripEventType;
import com.projectx.common.events.ChildTripStatusChangedEvent;
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
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.domain.TripDirection;
import com.projectx.tracking.repo.TripRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lifecycle of an {@link Arrival}: opened by a driver event, confirmed by school attendance,
 * approved/disputed by a guardian. Three independent actors can touch the same row, so every
 * mutation goes through {@link #inTx} - a transaction that retries on optimistic-lock conflicts
 * (the arrival is re-read fresh on each attempt) instead of surfacing a 409 to a parent who just
 * tapped "It's wrong". Deliberately not class-level @Transactional for that reason.
 */
@Service
public class ArrivalService {

    private static final Logger log = LoggerFactory.getLogger(ArrivalService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_TRIP_ARRIVALS = 500;
    private static final int MAX_PENDING_FOR_PARENT = 50;
    private static final int PENDING_LOOKBACK_DAYS = 7;
    private static final int BACKFILL_LOOKBACK_DAYS = 2;

    private final ArrivalRepository arrivalRepository;
    private final StudentRepository studentRepository;
    private final TripRepository tripRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final GuardianAccessService guardianAccessService;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate tx;

    public ArrivalService(ArrivalRepository arrivalRepository, StudentRepository studentRepository,
                          TripRepository tripRepository, ChildRepository childRepository,
                          ParentProfileRepository parentProfileRepository, DriverProfileRepository driverProfileRepository,
                          GuardianAccessService guardianAccessService, JdbcTemplate jdbc,
                          ApplicationEventPublisher eventPublisher, PlatformTransactionManager transactionManager) {
        this.arrivalRepository = arrivalRepository;
        this.studentRepository = studentRepository;
        this.tripRepository = tripRepository;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.guardianAccessService = guardianAccessService;
        this.jdbc = jdbc;
        this.eventPublisher = eventPublisher;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ---------------------------------------------------------------- driver side

    /** BOARDED only opens an arrival on a morning (home pickup) trip; on the afternoon trip the school pickup is the verifiable moment. */
    static ArrivalKind kindFor(ChildTripEventType type, TripDirection direction) {
        return switch (type) {
            case BOARDED -> direction == TripDirection.MORNING_PICKUP ? ArrivalKind.HOME_PICKUP : null;
            case DROPPED_AT_SCHOOL -> ArrivalKind.SCHOOL_DROP;
            case PICKED_FROM_SCHOOL -> ArrivalKind.SCHOOL_PICKUP;
            case DROPPED_AT_HOME, ABSENT -> null;
        };
    }

    /**
     * Idempotent: a replayed event finds the existing (trip, child, kind) row and does nothing, except that a
     * strictly later event time moves driverEventAt forward. After the arrival is committed the stored
     * attendance is read once more ({@link #reconcileOne}) - closing the race where the teacher's bulk sheet
     * committed between our attendance read and our insert, which would otherwise leave school_confirm
     * PENDING forever (the bulk's sync pass could not see an arrival that was not committed yet).
     */
    public void openFromDriverEvent(ChildTripStatusChangedEvent event) {
        Long arrivalId = inTx(() -> openOrAdvance(event));
        if (arrivalId != null) {
            reconcileOne(arrivalId);
        }
    }

    /**
     * Outbox-poller entry point (see {@code ArrivalReconciler#processOutbox} and module-tracking's
     * {@code ArrivalOutbox}): identical arrival-opening/advancing logic to {@link #openFromDriverEvent},
     * but marks the outbox row processed in the SAME transaction that creates/advances the Arrival -
     * the durability guarantee the outbox exists for (no window where the Arrival exists but the
     * outbox row still looks unprocessed, or vice versa - a crash between the two would otherwise
     * reintroduce exactly the loss window the outbox is meant to close). Safe to race with the fast
     * AFTER_COMMIT listener calling {@link #openFromDriverEvent} concurrently for the same event: both
     * paths go through the same idempotent find-or-create plus the DB's {@code uq_arrival_trip_child_kind}
     * unique constraint, so at most one Arrival is ever created for a given (trip, child, kind) - the
     * loser's transaction (including its outbox update) simply rolls back and is retried on the next
     * poll, at which point it finds the winner's row and marks itself processed without inserting again.
     */
    public void processOutboxEvent(long outboxId, ChildTripStatusChangedEvent event) {
        Long arrivalId = inTx(() -> {
            Long id = openOrAdvance(event);
            jdbc.update("UPDATE arrival_outbox SET processed_at = now() WHERE id = ? AND processed_at IS NULL", outboxId);
            return id;
        });
        if (arrivalId != null) {
            reconcileOne(arrivalId);
        }
    }

    private Long openOrAdvance(ChildTripStatusChangedEvent event) {
        Trip trip = tripRepository.findById(event.tripId()).orElse(null);
        if (trip == null) {
            return null;
        }
        ArrivalKind kind = kindFor(event.type(), trip.getDirection());
        if (kind == null) {
            return null;
        }
        Arrival existing = arrivalRepository.findByTripIdAndChildIdAndKind(trip.getId(), event.childId(), kind).orElse(null);
        if (existing != null) {
            if (existing.advanceDriverEventAt(event.occurredAt())) {
                arrivalRepository.saveAndFlush(existing);
                eventPublisher.publishEvent(new ArrivalChangedEvent(ArrivalResponse.from(existing), existing.getOutcome()));
            }
            return existing.getId();
        }
        Child child = childRepository.findById(event.childId()).orElse(null);
        if (child == null) {
            return null;
        }
        Student student = studentRepository.findByChildId(child.getId()).orElse(null);
        Arrival arrival = new Arrival(trip.getSchoolId(), trip.getId(), child.getId(), student == null ? null : student.getId(),
                child.getFirstName() + " " + child.getLastName(), kind, trip.getTripDate(), event.occurredAt());
        if (student != null) {
            // The school may have taken attendance before the bus event arrived (e.g. ABSENT marked at 8:00, bus drops at 8:20).
            findAttendance(student.getId(), trip.getTripDate()).ifPresent(status -> arrival.applySchoolConfirm(toConfirm(status)));
        }
        arrival.recomputeOutcome();
        Arrival saved = arrivalRepository.saveAndFlush(arrival);
        eventPublisher.publishEvent(new ArrivalChangedEvent(ArrivalResponse.from(saved), null));
        return saved.getId();
    }

    /**
     * Brings one arrival in line with the world as stored right now: resolves a missing studentId (the child
     * may have been linked to a roster student after the driver event) and re-derives school_confirm from
     * school_attendance. Idempotent; publishes (and returns true) only when something actually changed.
     */
    public boolean reconcileOne(Long arrivalId) {
        Boolean changed = inTx(() -> {
            Arrival arrival = arrivalRepository.findById(arrivalId).orElse(null);
            if (arrival == null) {
                return false;
            }
            boolean dirty = false;
            ArrivalOutcome before = arrival.getOutcome();
            if (arrival.getStudentId() == null) {
                Student student = studentRepository.findByChildId(arrival.getChildId()).orElse(null);
                if (student != null) {
                    arrival.attachStudent(student.getId());
                    dirty = true;
                }
            }
            if (arrival.getStudentId() != null) {
                var status = findAttendance(arrival.getStudentId(), arrival.getArrivalDate());
                if (status.isPresent() && arrival.getSchoolConfirm() != toConfirm(status.get())) {
                    arrival.applySchoolConfirm(toConfirm(status.get()));
                    dirty = true;
                }
            }
            if (dirty) {
                arrivalRepository.saveAndFlush(arrival);
                eventPublisher.publishEvent(new ArrivalChangedEvent(ArrivalResponse.from(arrival), before));
            }
            return dirty;
        });
        return Boolean.TRUE.equals(changed);
    }

    /** A roster student was just linked to a child: back-fill studentId + school confirmation on that child's recent arrivals. */
    public void backfillLinkedChild(Long childId) {
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM arrival_verifications WHERE child_id = ? AND student_id IS NULL AND arrival_date >= ? ORDER BY id LIMIT 200",
                Long.class, childId, java.sql.Date.valueOf(SchoolTime.today().minusDays(BACKFILL_LOOKBACK_DAYS)));
        ids.forEach(this::reconcileOne);
    }

    // ---------------------------------------------------------------- school side

    /**
     * Called after a bulk attendance write. Re-derives school confirmation for every arrival of the
     * marked students on that date from the <em>stored</em> attendance (so resending an unchanged
     * sheet still heals an arrival that missed its update), and publishes only real changes.
     */
    public void syncSchoolConfirmations(Map<Long, AttendanceStatus> statusByStudent, LocalDate date) {
        if (statusByStudent.isEmpty()) {
            return;
        }
        inTx(() -> {
            // Re-read what is STORED (not the request's map): with two overlapping sheets the later committer
            // wins in the table, and this pass must follow the table, not whichever request got here last.
            Map<Long, AttendanceStatus> stored = storedStatuses(statusByStudent.keySet(), date);
            for (Arrival arrival : arrivalRepository.findByStudentIdInAndArrivalDate(statusByStudent.keySet(), date)) {
                AttendanceStatus status = stored.getOrDefault(arrival.getStudentId(), statusByStudent.get(arrival.getStudentId()));
                SchoolConfirm confirm = toConfirm(status);
                if (arrival.getSchoolConfirm() == confirm) {
                    continue;
                }
                ArrivalOutcome before = arrival.getOutcome();
                arrival.applySchoolConfirm(confirm);
                arrivalRepository.saveAndFlush(arrival);
                eventPublisher.publishEvent(new ArrivalChangedEvent(ArrivalResponse.from(arrival), before));
            }
            return null;
        });
    }

    public PageResponse<ArrivalResponse> listForSchool(Long schoolId, LocalDate date, ArrivalOutcome outcome, Integer page, Integer size) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        LocalDate day = date != null ? date : SchoolTime.today();
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), StudentService.clampSize(size),
                Sort.by(Sort.Order.desc("driverEventAt"), Sort.Order.desc("id")));
        var result = outcome == null
                ? arrivalRepository.findBySchoolIdAndArrivalDate(schoolId, day, pageable)
                : arrivalRepository.findBySchoolIdAndArrivalDateAndOutcome(schoolId, day, outcome, pageable);
        return PageResponse.from(result.map(ArrivalResponse::from));
    }

    // ---------------------------------------------------------------- driver listing

    public List<ArrivalResponse> listForTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> ResourceNotFoundException.of("Trip", tripId));
        CurrentUserContext.requireSchoolAccess(trip.getSchoolId());
        CurrentUser user = CurrentUserContext.get();
        if (user.role() == Role.DRIVER) {
            Long driverId = driverProfileRepository.findByUserId(user.userId())
                    .orElseThrow(() -> new ForbiddenOperationException("No driver profile for current user")).getId();
            if (!trip.getDriverId().equals(driverId)) {
                throw new ForbiddenOperationException("Not the driver assigned to this trip");
            }
        }
        boolean driver = user.role() == Role.DRIVER;
        return arrivalRepository.findByTripId(tripId, PageRequest.of(0, MAX_TRIP_ARRIVALS, Sort.by("driverEventAt", "id")))
                .stream().map(ArrivalResponse::from)
                // A guardian's note is addressed to the school, not to the driver.
                .map(a -> driver ? a.withoutParentNote() : a).toList();
    }

    // ---------------------------------------------------------------- parent side

    public List<ArrivalResponse> pendingForMe() {
        ParentProfile parent = currentParent();
        List<Long> childIds = childRepository.findByGuardianParentId(parent.getId()).stream().map(Child::getId).toList();
        if (childIds.isEmpty()) {
            return List.of();
        }
        LocalDate since = SchoolTime.today().minusDays(PENDING_LOOKBACK_DAYS);
        return arrivalRepository.findPendingForChildren(childIds, since, PageRequest.of(0, MAX_PENDING_FOR_PARENT))
                .stream().map(ArrivalResponse::from).toList();
    }

    public ArrivalResponse respond(Long arrivalId, RespondRequest request) {
        if (request.response() != ParentResponse.APPROVED && request.response() != ParentResponse.DISPUTED) {
            throw new IllegalArgumentException("response must be APPROVED or DISPUTED");
        }
        ParentProfile parent = currentParent();
        return inTx(() -> {
            Arrival arrival = arrivalRepository.findById(arrivalId).orElseThrow(() -> ResourceNotFoundException.of("Arrival", arrivalId));
            Child child = childRepository.findById(arrival.getChildId()).orElseThrow(() -> ResourceNotFoundException.of("Arrival", arrivalId));
            if (!guardianAccessService.isGuardian(child, parent.getId())) {
                throw new ForbiddenOperationException("Not a guardian of this child");
            }
            if (arrival.getParentResponse() == request.response()) {
                return ArrivalResponse.from(arrival);
            }
            if (arrival.getParentResponse() == ParentResponse.DISPUTED) {
                // A dispute is a safety signal: another guardian approving afterwards must not silently erase it.
                throw new ConflictException("This arrival has been reported as wrong and is under review by the school");
            }
            ArrivalOutcome before = arrival.getOutcome();
            arrival.applyParentResponse(request.response(), blankToNull(request.note()));
            arrivalRepository.saveAndFlush(arrival);
            eventPublisher.publishEvent(new ArrivalChangedEvent(ArrivalResponse.from(arrival), before));
            return ArrivalResponse.from(arrival);
        });
    }

    // ---------------------------------------------------------------- helpers

    private ParentProfile currentParent() {
        return parentProfileRepository.findByUserId(CurrentUserContext.get().userId())
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for current user"));
    }

    private java.util.Optional<AttendanceStatus> findAttendance(Long studentId, LocalDate date) {
        List<String> rows = jdbc.queryForList(
                "SELECT status FROM school_attendance WHERE student_id = ? AND attendance_date = ?", String.class, studentId, date);
        return rows.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(AttendanceStatus.valueOf(rows.get(0)));
    }

    private Map<Long, AttendanceStatus> storedStatuses(java.util.Collection<Long> studentIds, LocalDate date) {
        String ids = "{" + studentIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "}";
        Map<Long, AttendanceStatus> out = new java.util.HashMap<>();
        jdbc.query("SELECT student_id, status FROM school_attendance WHERE student_id = ANY(?::bigint[]) AND attendance_date = ?",
                rs -> {
                    out.put(rs.getLong(1), AttendanceStatus.valueOf(rs.getString(2)));
                }, ids, java.sql.Date.valueOf(date));
        return out;
    }

    static SchoolConfirm toConfirm(AttendanceStatus status) {
        return switch (status) {
            case PRESENT, LATE -> SchoolConfirm.PRESENT;
            case ABSENT -> SchoolConfirm.ABSENT;
        };
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Runs {@code work} in its own transaction, re-running it on a lost optimistic-lock race (fresh reads each attempt). */
    private <T> T inTx(java.util.function.Supplier<T> work) {
        ObjectOptimisticLockingFailureException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return tx.execute(status -> work.get());
            } catch (ObjectOptimisticLockingFailureException e) {
                last = e;
                log.info("Arrival update lost an optimistic-lock race (attempt {}/{}), retrying", attempt, MAX_ATTEMPTS);
            }
        }
        throw last;
    }

}
