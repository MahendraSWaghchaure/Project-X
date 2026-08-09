package com.projectx.notification.listener;

import com.projectx.common.events.ChildTripEventType;
import com.projectx.common.events.ChildTripStatusChangedEvent;
import com.projectx.notification.service.NotificationDispatchService;
import com.projectx.people.domain.Child;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Boarding/drop-off confirmations, driver- or school-admin-marked, get relayed to the child's guardians. DROPPED_AT_HOME is the parent's own action, so it's skipped here - notifying them of their own confirmation would be noise. */
@Component
public class ChildTripStatusListener {

    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final NotificationDispatchService dispatchService;

    public ChildTripStatusListener(ChildRepository childRepository, ParentProfileRepository parentProfileRepository,
                                    NotificationDispatchService dispatchService) {
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.dispatchService = dispatchService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChildTripStatusChanged(ChildTripStatusChangedEvent event) {
        if (event.type() == ChildTripEventType.DROPPED_AT_HOME) {
            return;
        }
        Child child = childRepository.findById(event.childId()).orElse(null);
        if (child == null) {
            return;
        }
        String subject = subjectFor(event.type());
        String body = subject + " - " + child.getFirstName() + " " + child.getLastName();
        notify(child.getPrimaryParentId(), event, subject, body);
        if (child.getSecondaryParentId() != null) {
            notify(child.getSecondaryParentId(), event, subject, body);
        }
    }

    private void notify(Long parentProfileId, ChildTripStatusChangedEvent event, String subject, String body) {
        parentProfileRepository.findById(parentProfileId)
                .ifPresent(parent -> dispatchService.dispatch(parent.getUserId(), "trip." + event.type(), subject, body));
    }

    private String subjectFor(ChildTripEventType type) {
        return switch (type) {
            case BOARDED -> "Boarded the bus";
            case DROPPED_AT_SCHOOL -> "Dropped off at school";
            case PICKED_FROM_SCHOOL -> "Picked up from school";
            case ABSENT -> "Marked absent for today's trip";
            case DROPPED_AT_HOME -> "Dropped off at home";
        };
    }
}
