package com.projectx.tracking.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.events.SosRaisedEvent;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.tracking.domain.SosEvent;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.dto.SosEventResponse;
import com.projectx.tracking.repo.SosEventRepository;
import com.projectx.tracking.ws.SosBroadcaster;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The panic button. Deliberately minimal-friction to trigger (driver-or-school-admin auth only,
 * no extra confirmation step) since the whole point is speed in a genuine emergency; resolving is
 * restricted to school staff so a driver can't quietly dismiss their own alert.
 */
@Service
@Transactional
public class SosService {

    private final TripService tripService;
    private final SosEventRepository sosEventRepository;
    private final SosBroadcaster sosBroadcaster;
    private final ApplicationEventPublisher eventPublisher;

    public SosService(TripService tripService, SosEventRepository sosEventRepository, SosBroadcaster sosBroadcaster,
                       ApplicationEventPublisher eventPublisher) {
        this.tripService = tripService;
        this.sosEventRepository = sosEventRepository;
        this.sosBroadcaster = sosBroadcaster;
        this.eventPublisher = eventPublisher;
    }

    public SosEventResponse trigger(Long tripId, String note) {
        Trip trip = tripService.findAndAuthorizeAsDriverOrSchoolAdmin(tripId);
        Long triggeredByUserId = CurrentUserContext.get().userId();

        SosEvent sosEvent = sosEventRepository.save(
                new SosEvent(trip.getId(), trip.getVehicleId(), trip.getSchoolId(), trip.getRouteId(), triggeredByUserId, note));
        SosEventResponse response = SosEventResponse.from(sosEvent);

        sosBroadcaster.broadcast(response);
        eventPublisher.publishEvent(new SosRaisedEvent(sosEvent.getId(), trip.getId(), trip.getVehicleId(),
                trip.getSchoolId(), trip.getRouteId(), triggeredByUserId, note, Instant.now()));
        return response;
    }

    @Transactional(readOnly = true)
    public List<SosEventResponse> listActiveForSchool(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return sosEventRepository.findBySchoolIdAndResolvedAtIsNull(schoolId).stream()
                .map(SosEventResponse::from)
                .toList();
    }

    public SosEventResponse resolve(Long sosEventId) {
        SosEvent sosEvent = sosEventRepository.findById(sosEventId)
                .orElseThrow(() -> ResourceNotFoundException.of("SosEvent", sosEventId));
        CurrentUserContext.requireSchoolAccess(sosEvent.getSchoolId());
        if (CurrentUserContext.get().role() == com.projectx.common.security.Role.DRIVER) {
            throw new ForbiddenOperationException("A driver cannot resolve their own SOS alert");
        }
        sosEvent.resolve(CurrentUserContext.get().userId());
        SosEventResponse response = SosEventResponse.from(sosEvent);
        sosBroadcaster.broadcast(response);
        eventPublisher.publishEvent(AuditEvent.of("SOS_RESOLVED", "SOS_EVENT", sosEvent.getId(), sosEvent.getSchoolId(),
                "SOS on trip " + sosEvent.getTripId() + " marked resolved"));
        return response;
    }
}
