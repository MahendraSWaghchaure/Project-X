package com.projectx.notification.web;

import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.notification.dto.NotificationRecordResponse;
import com.projectx.notification.repo.NotificationRecordRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationRecordRepository notificationRecordRepository;

    public NotificationController(NotificationRecordRepository notificationRecordRepository) {
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @GetMapping("/notifications/me")
    public ApiResponse<PageResponse<NotificationRecordResponse>> myNotifications(@PageableDefault(size = 30) Pageable pageable) {
        Long userId = CurrentUserContext.get().userId();
        return ApiResponse.ok(PageResponse.from(
                notificationRecordRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable).map(NotificationRecordResponse::from)));
    }
}
