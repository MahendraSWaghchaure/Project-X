package com.projectx.notification.listener;

import com.projectx.common.events.GeofenceRaisedEvent;
import com.projectx.notification.service.NotificationDispatchService;
import com.projectx.people.domain.Child;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.school.repo.SchoolAdminProfileRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns tracking geofence crossings into parent/school-admin alerts. Runs AFTER_COMMIT so we
 * never notify about an event whose transaction ultimately rolled back.
 */
@Component
public class GeofenceEventListener {

    private final NotificationDispatchService dispatchService;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final GuardianAccessService guardianAccessService;
    private final SchoolAdminProfileRepository schoolAdminProfileRepository;

    public GeofenceEventListener(NotificationDispatchService dispatchService, ChildRepository childRepository,
                                  ParentProfileRepository parentProfileRepository,
                                  GuardianAccessService guardianAccessService,
                                  SchoolAdminProfileRepository schoolAdminProfileRepository) {
        this.dispatchService = dispatchService;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.guardianAccessService = guardianAccessService;
        this.schoolAdminProfileRepository = schoolAdminProfileRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGeofenceRaised(GeofenceRaisedEvent event) {
        switch (event.type()) {
            case APPROACHING_STOP, ARRIVED_STOP -> notifyChildGuardians(event, childForStop(event.childId()));
            case APPROACHING_SCHOOL, ARRIVED_SCHOOL -> childrenOnRoute(event.routeId())
                    .forEach(child -> notifyChildGuardians(event, child));
            case END_OF_DAY_DELAY -> {
                childrenOnRoute(event.routeId()).forEach(child -> notifyChildGuardians(event, child));
                notifySchoolAdmins(event);
            }
        }
    }

    private List<Child> childrenOnRoute(Long routeId) {
        return routeId != null ? childRepository.findByRouteId(routeId) : List.of();
    }

    private Child childForStop(Long childId) {
        return childId != null ? childRepository.findById(childId).orElse(null) : null;
    }

    private void notifyChildGuardians(GeofenceRaisedEvent event, Child child) {
        if (child == null) {
            return;
        }
        String subject = subjectFor(event);
        String body = subject + " (" + child.getFirstName() + " " + child.getLastName() + ")";
        guardianAccessService.allGuardianIds(child).forEach(parentId -> dispatchToParentAccount(parentId, event, subject, body));
    }

    private void dispatchToParentAccount(Long parentProfileId, GeofenceRaisedEvent event, String subject, String body) {
        parentProfileRepository.findById(parentProfileId)
                .ifPresent(parent -> dispatchService.dispatch(parent.getUserId(), "geofence." + event.type(), subject, body));
    }

    private void notifySchoolAdmins(GeofenceRaisedEvent event) {
        schoolAdminProfileRepository.findBySchoolId(event.schoolId())
                .forEach(admin -> dispatchService.dispatch(admin.getUserId(), "geofence." + event.type(),
                        "Delayed drop-off trip", "A drop-off trip is running significantly past dismissal time."));
    }

    private String subjectFor(GeofenceRaisedEvent event) {
        return switch (event.type()) {
            case APPROACHING_SCHOOL -> "Bus approaching school";
            case ARRIVED_SCHOOL -> "Bus arrived at school";
            case APPROACHING_STOP -> "Bus approaching your stop";
            case ARRIVED_STOP -> "Bus arrived at your stop";
            case END_OF_DAY_DELAY -> "Drop-off running late";
        };
    }
}
