package com.projectx.notification.service;

import com.projectx.notification.domain.PushDeviceToken;
import com.projectx.notification.repo.PushDeviceTokenRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers/looks up the one FCM device token on file for a user - see FirebasePushSender (the consumer) and NotificationController (the registration endpoint). */
@Service
@Transactional
public class DeviceTokenService {

    private final PushDeviceTokenRepository repository;

    public DeviceTokenService(PushDeviceTokenRepository repository) {
        this.repository = repository;
    }

    /** Upserts the current device token for a user - the frontend calls this on every app start/login, so the latest device always wins. */
    public void register(Long userId, String deviceToken) {
        repository.findById(userId)
                .ifPresentOrElse(existing -> existing.updateToken(deviceToken),
                        () -> repository.save(new PushDeviceToken(userId, deviceToken)));
    }

    @Transactional(readOnly = true)
    public Optional<String> findToken(Long userId) {
        return repository.findById(userId).map(PushDeviceToken::getDeviceToken);
    }
}
