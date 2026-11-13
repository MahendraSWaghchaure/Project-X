package com.projectx.notification.repo;

import com.projectx.notification.domain.PushDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, Long> {
}
