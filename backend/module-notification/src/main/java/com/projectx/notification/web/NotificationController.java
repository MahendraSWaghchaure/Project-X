package com.projectx.notification.web;

import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.notification.dto.NotificationRecordResponse;
import com.projectx.notification.dto.RegisterDeviceTokenRequest;
import com.projectx.notification.repo.NotificationRecordRepository;
import com.projectx.notification.service.DeviceTokenService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationRecordRepository notificationRecordRepository;
    private final DeviceTokenService deviceTokenService;

    public NotificationController(NotificationRecordRepository notificationRecordRepository, DeviceTokenService deviceTokenService) {
        this.notificationRecordRepository = notificationRecordRepository;
        this.deviceTokenService = deviceTokenService;
    }

    @GetMapping("/notifications/me")
    public ApiResponse<PageResponse<NotificationRecordResponse>> myNotifications(@PageableDefault(size = 30) Pageable pageable) {
        Long userId = CurrentUserContext.get().userId();
        return ApiResponse.ok(PageResponse.from(
                notificationRecordRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable).map(NotificationRecordResponse::from)));
    }

    /** Lets the frontend register/refresh the current user's FCM device token - backend-only wiring; see FirebasePushSender. */
    @PostMapping("/notifications/me/device-token")
    public ApiResponse<Void> registerDeviceToken(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        deviceTokenService.register(CurrentUserContext.get().userId(), request.token());
        return ApiResponse.ok(null);
    }
}
