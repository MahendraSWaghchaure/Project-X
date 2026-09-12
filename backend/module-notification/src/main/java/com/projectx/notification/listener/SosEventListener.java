package com.projectx.notification.listener;

import com.projectx.common.events.SosRaisedEvent;
import com.projectx.notification.service.NotificationDispatchService;
import com.projectx.people.domain.Child;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.school.repo.SchoolAdminProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The highest-priority alert in the system - every guardian of every child on the vehicle, plus
 * every school admin, gets notified the instant a driver hits the panic button. Runs AFTER_COMMIT
 * like every other listener here, but nothing else in the notification pipeline should ever be
 * allowed to throttle or delay this one (see the async-dispatch priority note in ARCHITECTURE.md).
 */
@Component
public class SosEventListener {

    private final NotificationDispatchService dispatchService;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final GuardianAccessService guardianAccessService;
    private final SchoolAdminProfileRepository schoolAdminProfileRepository;

    public SosEventListener(NotificationDispatchService dispatchService, ChildRepository childRepository,
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
    public void onSosRaised(SosRaisedEvent event) {
        String subject = "SOS alert on your child's route";
        for (Child child : childRepository.findByRouteId(event.routeId())) {
            String body = subject + " (" + child.getFirstName() + " " + child.getLastName() + ")"
                    + (event.note() != null && !event.note().isBlank() ? " - " + event.note() : "");
            guardianAccessService.allGuardianIds(child).forEach(parentId -> dispatchToParent(parentId, subject, body));
        }

        String adminBody = "SOS triggered on trip #" + event.tripId()
                + (event.note() != null && !event.note().isBlank() ? " - " + event.note() : "");
        schoolAdminProfileRepository.findBySchoolId(event.schoolId())
                .forEach(admin -> dispatchService.dispatch(admin.getUserId(), "sos.raised", "SOS alert", adminBody));
    }

    private void dispatchToParent(Long parentProfileId, String subject, String body) {
        parentProfileRepository.findById(parentProfileId)
                .ifPresent(parent -> dispatchService.dispatch(parent.getUserId(), "sos.raised", subject, body));
    }
}
