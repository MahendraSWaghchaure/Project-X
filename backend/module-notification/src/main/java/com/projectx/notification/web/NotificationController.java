package com.projectx.notification.web;

import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.web.ApiResponse;
import com.projectx.notification.dto.NotificationRecordResponse;
import com.projectx.notification.repo.NotificationRecordRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationRecordRepository notificationRecordRepository;

    public NotificationController(NotificationRecordRepository notificationRecordRepository) {
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @GetMapping("/notifications/me")
    public ApiResponse<List<NotificationRecordResponse>> myNotifications() {
        Long userId = CurrentUserContext.get().userId();
        return ApiResponse.ok(notificationRecordRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationRecordResponse::from)
                .toList());
    }
}
