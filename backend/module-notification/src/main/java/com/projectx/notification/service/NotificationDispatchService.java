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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The one place every alert in the system funnels through - every attempt, sent or not, is logged to NotificationRecord. */
@Service
@Transactional
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final UserAccountRepository userAccountRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final PushSender pushSender;

    public NotificationDispatchService(UserAccountRepository userAccountRepository,
                                        NotificationRecordRepository notificationRecordRepository,
                                        SmsSender smsSender, EmailSender emailSender, PushSender pushSender) {
        this.userAccountRepository = userAccountRepository;
        this.notificationRecordRepository = notificationRecordRepository;
        this.smsSender = smsSender;
        this.emailSender = emailSender;
        this.pushSender = pushSender;
    }

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

    private void attempt(Long userId, NotificationChannel channel, String templateKey, String body, Runnable action) {
        NotificationStatus status;
        String failureReason = null;
        try {
            action.run();
            status = NotificationStatus.SENT;
        } catch (ChannelNotConfiguredException e) {
            status = NotificationStatus.SKIPPED_NOT_CONFIGURED;
            failureReason = e.getMessage();
        } catch (Exception e) {
            log.error("Notification dispatch failed: user={} channel={}", userId, channel, e);
            status = NotificationStatus.FAILED;
            failureReason = e.getMessage();
        }
        notificationRecordRepository.save(new NotificationRecord(userId, channel, templateKey, body, status, failureReason));
    }
}
