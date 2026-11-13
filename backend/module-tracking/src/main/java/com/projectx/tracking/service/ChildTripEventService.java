package com.projectx.tracking.service;

import com.projectx.common.events.ChildTripEventType;
import com.projectx.common.events.ChildTripStatusChangedEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.ChildQrTokenService;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.tracking.domain.ArrivalOutbox;
import com.projectx.tracking.domain.ChildTripEvent;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.dto.ChildTripEventResponse;
import com.projectx.tracking.repo.ArrivalOutboxRepository;
import com.projectx.tracking.repo.ChildTripEventRepository;
import com.projectx.tracking.repo.TripRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BOARDED/DROPPED_AT_SCHOOL/PICKED_FROM_SCHOOL/ABSENT are the driver's (or school admin's) call;
 * DROPPED_AT_HOME is deliberately the parent's alone - "once the child reaches home safely the
 * parent marks it," per the product spec, mirroring the school-arrival confirmation but from the
 * other end of the trip.
 */
@Service
@Transactional
public class ChildTripEventService {

    private static final Set<ChildTripEventType> DRIVER_MARKED_TYPES = EnumSet.of(
            ChildTripEventType.BOARDED, ChildTripEventType.DROPPED_AT_SCHOOL,
            ChildTripEventType.PICKED_FROM_SCHOOL, ChildTripEventType.ABSENT);

    private final TripRepository tripRepository;
    private final ChildTripEventRepository childTripEventRepository;
    private final ArrivalOutboxRepository arrivalOutboxRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final TripService tripService;
    private final ApplicationEventPublisher eventPublisher;
    private final ChildQrTokenService childQrTokenService;
    private final GuardianAccessService guardianAccessService;

    public ChildTripEventService(TripRepository tripRepository, ChildTripEventRepository childTripEventRepository,
                                  ArrivalOutboxRepository arrivalOutboxRepository, ChildRepository childRepository,
                                  ParentProfileRepository parentProfileRepository,
                                  TripService tripService, ApplicationEventPublisher eventPublisher,
                                  ChildQrTokenService childQrTokenService, GuardianAccessService guardianAccessService) {
        this.tripRepository = tripRepository;
        this.childTripEventRepository = childTripEventRepository;
        this.arrivalOutboxRepository = arrivalOutboxRepository;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.tripService = tripService;
        this.eventPublisher = eventPublisher;
        this.childQrTokenService = childQrTokenService;
        this.guardianAccessService = guardianAccessService;
    }

    public ChildTripEventResponse recordDriverMarkedEvent(Long tripId, Long childId, ChildTripEventType type) {
        if (!DRIVER_MARKED_TYPES.contains(type)) {
            throw new IllegalArgumentException(type + " must be recorded by the parent, not the driver");
        }
        Trip trip = tripService.findAndAuthorizeAsDriverOrSchoolAdmin(tripId);
        return record(trip, childId, type, CurrentUserContext.get().userId());
    }

    /** Same as recordDriverMarkedEvent, but the child is identified by a scanned QR token instead of a manual pick from a list. */
    public ChildTripEventResponse recordScannedEvent(Long tripId, String qrToken, ChildTripEventType type) {
        Long childId = childQrTokenService.resolveChildId(qrToken);
        return recordDriverMarkedEvent(tripId, childId, type);
    }

    public ChildTripEventResponse recordParentHomeArrival(Long tripId, Long childId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> ResourceNotFoundException.of("Trip", tripId));
        Long userId = CurrentUserContext.get().userId();
        ParentProfile parent = parentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for current user"));
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!guardianAccessService.isGuardian(child, parent.getId())) {
            throw new ForbiddenOperationException("Not a guardian of this child");
        }
        if (!child.getSchoolId().equals(trip.getSchoolId())) {
            throw new ForbiddenOperationException("Child does not belong to this trip's school");
        }

        return record(trip, childId, ChildTripEventType.DROPPED_AT_HOME, userId);
    }

    @Transactional(readOnly = true)
    public List<ChildTripEventResponse> listForTrip(Long tripId) {
        return childTripEventRepository.findByTripId(tripId).stream().map(ChildTripEventResponse::from).toList();
    }

    /** Cross-trip attendance history for one child, for the parent-facing calendar view - not just one trip's events. */
    @Transactional(readOnly = true)
    public List<ChildTripEventResponse> getAttendanceHistory(Long childId, LocalDate from, LocalDate to) {
        Long userId = CurrentUserContext.get().userId();
        ParentProfile parent = parentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for current user"));
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!guardianAccessService.isGuardian(child, parent.getId())) {
            throw new ForbiddenOperationException("Not a guardian of this child");
        }
        Instant fromInstant = from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        return childTripEventRepository.findByChildIdAndOccurredAtBetweenOrderByOccurredAtDesc(childId, fromInstant, toInstant)
                .stream().map(ChildTripEventResponse::from).toList();
    }

    /**
     * Persists the event and, in the SAME transaction, a durable outbox row recording "an Arrival
     * may need to be opened for this" - the outbox write can never be lost separately from the
     * event it mirrors, closing the gap the old design had (an AFTER_COMMIT listener that might
     * simply never run, leaving only a 60s sweep as the backstop). See module-attendance's
     * ArrivalReconciler#processOutbox for the consumer side.
     */
    private ChildTripEventResponse record(Trip trip, Long childId, ChildTripEventType type, Long confirmedByUserId) {
        if (childTripEventRepository.existsByTripIdAndChildIdAndEventType(trip.getId(), childId, type)) {
            throw new ConflictException(type + " already recorded for this child on this trip");
        }
        ChildTripEvent event = childTripEventRepository.save(new ChildTripEvent(trip.getId(), childId, type, confirmedByUserId));
        arrivalOutboxRepository.save(new ArrivalOutbox(event.getId(), trip.getId(), childId, trip.getSchoolId(),
                type.name(), confirmedByUserId, event.getOccurredAt()));
        eventPublisher.publishEvent(new ChildTripStatusChangedEvent(
                trip.getId(), childId, trip.getSchoolId(), type, confirmedByUserId, event.getOccurredAt()));
        return ChildTripEventResponse.from(event);
    }
}
