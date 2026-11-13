package com.projectx.notification.service;

import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.notification.domain.NotificationChannel;
import com.projectx.notification.domain.NotificationRecord;
import com.projectx.notification.domain.NotificationStatus;
import com.projectx.notification.repo.NotificationRecordRepository;
import com.projectx.notification.sender.ChannelNotConfiguredException;
import com.projectx.notification.sender.EmailSender;
import com.projectx.notification.sender.PushSender;
import com.projectx.notification.sender.SmsSender;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place every alert in the system funnels through - every attempt, sent or not, is logged
 * to NotificationRecord. dispatch() itself is @Async: callers (all AFTER_COMMIT event listeners,
 * so no live transaction is riding on it) hand off to the notificationExecutor pool and return
 * immediately, so one slow provider call never blocks the request/event thread that raised it.
 */
@Service
@Transactional
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final UserAccountRepository userAccountRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final PushSender pushSender;
    private final RetryingChannelDispatcher retryingChannelDispatcher;

    public NotificationDispatchService(UserAccountRepository userAccountRepository,
                                        NotificationRecordRepository notificationRecordRepository,
                                        SmsSender smsSender, EmailSender emailSender, PushSender pushSender,
                                        RetryingChannelDispatcher retryingChannelDispatcher) {
        this.userAccountRepository = userAccountRepository;
        this.notificationRecordRepository = notificationRecordRepository;
        this.smsSender = smsSender;
        this.emailSender = emailSender;
        this.pushSender = pushSender;
        this.retryingChannelDispatcher = retryingChannelDispatcher;
    }

    @Async("notificationExecutor")
    public void dispatch(Long recipientUserId, String templateKey, String subject, String body) {
        UserAccount user = userAccountRepository.findById(recipientUserId).orElse(null);
        if (user == null) {
            log.warn("Cannot dispatch notification - no such user {}", recipientUserId);
            return;
        }

        attempt(recipientUserId, NotificationChannel.PUSH, templateKey, body, () -> pushSender.send(recipientUserId, subject, body));
        if (user.getPhone() != null) {
            attempt(recipientUserId, NotificationChannel.SMS, templateKey, body, () -> smsSender.send(user.getPhone(), body));
        }
        attempt(recipientUserId, NotificationChannel.EMAIL, templateKey, body, () -> emailSender.send(user.getEmail(), subject, body));
    }

    /**
     * SMS to a phone number that is not a user account (a guardian on the roster). Same retry/log pipeline as dispatch(),
     * SMS channel only, and the log row carries {@code loggedBody} (never the real body) plus a masked number - so a secret
     * embedded in the message (a single-use consent link) is not persisted in notification_records.
     */
    @Async("notificationExecutor")
    public void dispatchSmsToNumber(String phone, String templateKey, String body, String loggedBody) {
        NotificationStatus status;
        String failureReason = null;
        try {
            retryingChannelDispatcher.send(() -> smsSender.send(phone, body));
            status = NotificationStatus.SENT;
        } catch (ChannelNotConfiguredException e) {
            status = NotificationStatus.SKIPPED_NOT_CONFIGURED;
            failureReason = e.getMessage();
        } catch (Exception e) {
            log.error("External SMS dispatch exhausted retries: template={}", templateKey, e);
            status = NotificationStatus.DEAD_LETTERED;
            failureReason = e.getMessage();
        }
        notificationRecordRepository.save(NotificationRecord.external(maskPhone(phone), NotificationChannel.SMS, templateKey,
                loggedBody, status, failureReason));
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }

    /** Notifications every retry attempt failed for - the human-in-the-loop worklist for a real outage, not a transient blip. */
    @Transactional(readOnly = true)
    public List<NotificationRecord> listDeadLettered() {
        return notificationRecordRepository.findByStatus(NotificationStatus.DEAD_LETTERED);
    }

    private void attempt(Long userId, NotificationChannel channel, String templateKey, String body, Runnable action) {
        NotificationStatus status;
        String failureReason = null;
        try {
            retryingChannelDispatcher.send(action);
            status = NotificationStatus.SENT;
        } catch (ChannelNotConfiguredException e) {
            status = NotificationStatus.SKIPPED_NOT_CONFIGURED;
            failureReason = e.getMessage();
        } catch (Exception e) {
            log.error("Notification dispatch exhausted retries: user={} channel={}", userId, channel, e);
            status = NotificationStatus.DEAD_LETTERED;
            failureReason = e.getMessage();
        }
        notificationRecordRepository.save(new NotificationRecord(userId, channel, templateKey, body, status, failureReason));
    }
}
