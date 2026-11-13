package com.projectx.hardware.repo;

import com.projectx.hardware.domain.DeviceApiKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceApiKeyRepository extends JpaRepository<DeviceApiKey, Long> {

    Optional<DeviceApiKey> findByDeviceIdAndActiveTrue(String deviceId);
}
