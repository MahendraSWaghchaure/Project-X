package com.projectx.notification.dto;

import com.projectx.notification.domain.NotificationChannel;
import com.projectx.notification.domain.NotificationRecord;
import com.projectx.notification.domain.NotificationStatus;
import java.time.Instant;

public record NotificationRecordResponse(
        Long id, NotificationChannel channel, String templateKey, NotificationStatus status, Instant createdAt) {

    public static NotificationRecordResponse from(NotificationRecord record) {
        return new NotificationRecordResponse(record.getId(), record.getChannel(), record.getTemplateKey(),
                record.getStatus(), record.getCreatedAt());
    }
}
