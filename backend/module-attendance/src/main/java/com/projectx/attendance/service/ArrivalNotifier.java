package com.projectx.attendance.service;

import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.domain.ParentResponse;
import com.projectx.attendance.domain.SchoolConfirm;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.notification.service.NotificationDispatchService;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.tracking.ws.StompRelayPublisher;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Fan-out of one Arrival change: live STOMP pushes (parent popup, school board, driver checklist)
 * through the shared Redis relay, plus SMS/email/push for the moments that matter - a mismatch
 * (school says ABSENT although the bus recorded the child) or a guardian dispute -> every guardian
 * (via GuardianAccessService.allGuardianIds, never just the primary) and the school admins; a school
 * confirmation -> the guardians. Runs on attendanceExecutor (MDC/X-Request-Id carried over), so
 * none of this is on the request path. Best-effort by design: failures are logged, never thrown -
 * the Arrival row itself is already committed and remains the source of truth.
 */
@Component
public class ArrivalNotifier {

    private static final Logger log = LoggerFactory.getLogger(ArrivalNotifier.class);
    private static final int MAX_SCHOOL_ADMINS_NOTIFIED = 20;

    private final StompRelayPublisher relayPublisher;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final GuardianAccessService guardianAccessService;
    private final UserAccountRepository userAccountRepository;
    private final NotificationDispatchService dispatchService;
    private final TeacherAlertService teacherAlertService;

    public ArrivalNotifier(StompRelayPublisher relayPublisher, ChildRepository childRepository,
                           ParentProfileRepository parentProfileRepository, GuardianAccessService guardianAccessService,
                           UserAccountRepository userAccountRepository, NotificationDispatchService dispatchService,
                           TeacherAlertService teacherAlertService) {
        this.relayPublisher = relayPublisher;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.guardianAccessService = guardianAccessService;
        this.userAccountRepository = userAccountRepository;
        this.dispatchService = dispatchService;
        this.teacherAlertService = teacherAlertService;
    }

    @Async("attendanceExecutor")
    public void fanOut(ArrivalChangedEvent event) {
        try {
            ArrivalResponse arrival = event.arrival();
            Set<Long> guardianUserIds = guardianUserIds(arrival.childId());

            for (Long userId : guardianUserIds) {
                relayPublisher.publish("/topic/parent/" + userId + "/arrivals", arrival);
            }
            relayPublisher.publish("/topic/school/" + arrival.schoolId() + "/arrivals", arrival);
            // The trip topic is what the driver sees: it never carries the guardian's private note.
            relayPublisher.publish("/topic/trip/" + arrival.tripId() + "/arrivals", arrival.withoutParentNote());
            // C4: the class teacher hears about "dropped by the bus but marked ABSENT" on their own topic.
            teacherAlertService.onArrivalChanged(arrival);

            notifyIfNeeded(event, guardianUserIds);
        } catch (Exception e) {
            log.error("Arrival fan-out failed for arrival {}", event.arrival().id(), e);
        }
    }

    private void notifyIfNeeded(ArrivalChangedEvent event, Set<Long> guardianUserIds) {
        ArrivalResponse a = event.arrival();
        ArrivalOutcome before = event.previousOutcome();

        if (a.outcome() == ArrivalOutcome.ALERT && before != ArrivalOutcome.ALERT) {
            String subject = "Safe-arrival alert - " + a.childName();
            String body = alertBody(a);
            Set<Long> recipients = new LinkedHashSet<>(guardianUserIds);
            recipients.addAll(schoolAdminUserIds(a.schoolId()));
            recipients.forEach(userId -> dispatchService.dispatch(userId, "arrival.ALERT", subject, body));
        } else if (a.outcome() == ArrivalOutcome.SAFE && before != ArrivalOutcome.SAFE && a.schoolConfirm() == SchoolConfirm.PRESENT) {
            String subject = "School confirmed arrival";
            String body = "The school has confirmed " + a.childName() + " is present today.";
            guardianUserIds.forEach(userId -> dispatchService.dispatch(userId, "arrival.SCHOOL_CONFIRMED", subject, body));
        }
    }

    private static String alertBody(ArrivalResponse a) {
        if (a.schoolConfirm() == SchoolConfirm.ABSENT) {
            String what = switch (a.kind()) {
                case SCHOOL_DROP -> "was dropped at school by the driver";
                case HOME_PICKUP -> "was picked up from home by the driver";
                case SCHOOL_PICKUP -> "was picked up from school by the driver";
            };
            return a.childName() + " " + what + " but school attendance marks them ABSENT. "
                    + "Please contact the school right away.";
        }
        if (a.parentResponse() == ParentResponse.DISPUTED) {
            return "A guardian reported that the " + describe(a.kind()) + " record for " + a.childName()
                    + " is wrong. The school has been alerted.";
        }
        return "Safe-arrival check for " + a.childName() + " needs attention.";
    }

    private static String describe(ArrivalKind kind) {
        return switch (kind) {
            case HOME_PICKUP -> "home pickup";
            case SCHOOL_DROP -> "school drop-off";
            case SCHOOL_PICKUP -> "school pickup";
        };
    }

    private Set<Long> guardianUserIds(Long childId) {
        Child child = childRepository.findById(childId).orElse(null);
        if (child == null) {
            return Set.of();
        }
        List<Long> parentIds = guardianAccessService.allGuardianIds(child);
        Set<Long> userIds = new LinkedHashSet<>();
        for (ParentProfile p : parentProfileRepository.findAllById(parentIds)) {
            userIds.add(p.getUserId());
        }
        return userIds;
    }

    private Set<Long> schoolAdminUserIds(Long schoolId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (UserAccount u : userAccountRepository
                .findBySchoolIdAndRole(schoolId, Role.SCHOOL_ADMIN, PageRequest.of(0, MAX_SCHOOL_ADMINS_NOTIFIED)).getContent()) {
            if (u.isActive()) {
                ids.add(u.getId());
            }
        }
        return ids;
    }
}
