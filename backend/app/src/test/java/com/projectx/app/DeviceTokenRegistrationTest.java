package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.notification.service.DeviceTokenService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/** Production-hardening part 3 (device token storage) - see PLAN-production-hardening.md section 3. */
class DeviceTokenRegistrationTest extends IntegrationTestBase {

    @Autowired
    DeviceTokenService deviceTokenService;

    @Test
    void registeringATokenPersistsItAndASecondCallUpsertsRatherThanDuplicating() {
        String token = platformAdminToken();
        Long userId = jwtSubject(token);

        ResponseEntity<ApiResponse<Void>> first = exchange("/notifications/me/device-token", HttpMethod.POST,
                Map.of("token", "device-token-one"), token, new ParameterizedTypeReference<>() {});
        assertSuccess(first);
        assertThat(deviceTokenService.findToken(userId)).contains("device-token-one");

        ResponseEntity<ApiResponse<Void>> second = exchange("/notifications/me/device-token", HttpMethod.POST,
                Map.of("token", "device-token-two"), token, new ParameterizedTypeReference<>() {});
        assertSuccess(second);
        assertThat(deviceTokenService.findToken(userId)).as("latest device wins").contains("device-token-two");
    }
}
