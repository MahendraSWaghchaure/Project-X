package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.hardware.domain.DeviceApiKey;
import com.projectx.hardware.domain.GateType;
import com.projectx.hardware.dto.IngestResultResponse;
import com.projectx.hardware.dto.RfidTapEventRequest;
import com.projectx.hardware.dto.VehicleTelemetrySampleRequest;
import com.projectx.hardware.repo.DeviceApiKeyRepository;
import com.projectx.hardware.repo.RfidTapEventRepository;
import com.projectx.hardware.repo.VehicleTelemetrySampleRepository;
import com.projectx.hardware.security.DeviceKeyHasher;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Full-stack proof for module-hardware (see HardwareIngestController's javadoc for what this
 * module does and deliberately does NOT do - no real RFID/telemetry hardware exists yet, this is
 * pure data-capture plumbing). There is no admin endpoint to provision a DeviceApiKey yet
 * (out of scope - see that javadoc), so this test inserts one directly via the repository, the
 * same way a real device would be provisioned by hand today.
 */
class HardwareIngestFlowTest extends IntegrationTestBase {

    @Autowired
    private DeviceApiKeyRepository deviceApiKeyRepository;
    @Autowired
    private RfidTapEventRepository tapEventRepository;
    @Autowired
    private VehicleTelemetrySampleRepository telemetrySampleRepository;

    private String provisionDevice(String deviceId, String rawKey) {
        deviceApiKeyRepository.save(new DeviceApiKey(deviceId, DeviceKeyHasher.sha256Hex(rawKey), "test device"));
        return rawKey;
    }

    private ResponseEntity<ApiResponse<IngestResultResponse>> postTap(String deviceId, String deviceKeyHeader, RfidTapEventRequest body) {
        return exchangeWithHeaders("/hardware/tap", HttpMethod.POST, body, null,
                Map.of("X-Device-Id", deviceId, "X-Device-Key", deviceKeyHeader), new ParameterizedTypeReference<>() {});
    }

    @Test
    void validKeyIsAcceptedAndStored() {
        String deviceId = unique("hw-tap-device-");
        provisionDevice(deviceId, "raw-key-" + deviceId);

        Instant timestamp = Instant.now();
        var response = postTap(deviceId, "raw-key-" + deviceId,
                new RfidTapEventRequest(deviceId, "card-123", GateType.HOME, timestamp));

        assertStatus(response, HttpStatus.ACCEPTED);
        assertThat(response.getBody().data().stored()).isTrue();
        assertThat(tapEventRepository.existsByDeviceIdAndEventTimestamp(deviceId, timestamp)).isTrue();
    }

    @Test
    void invalidKeyIsRejected() {
        String deviceId = unique("hw-tap-device-");
        provisionDevice(deviceId, "raw-key-" + deviceId);

        var response = postTap(deviceId, "totally-wrong-key",
                new RfidTapEventRequest(deviceId, "card-123", GateType.HOME, Instant.now()));

        assertStatus(response, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingKeyIsRejected() {
        String deviceId = unique("hw-tap-device-");
        provisionDevice(deviceId, "raw-key-" + deviceId);

        var response = exchangeWithHeaders("/hardware/tap", HttpMethod.POST,
                new RfidTapEventRequest(deviceId, "card-123", GateType.HOME, Instant.now()), null,
                Map.of(), new ParameterizedTypeReference<ApiResponse<IngestResultResponse>>() {});

        assertStatus(response, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void duplicateNaturalKeyDoesNotDoubleWrite() {
        String deviceId = unique("hw-tap-device-");
        String rawKey = provisionDevice(deviceId, "raw-key-" + deviceId);
        Instant timestamp = Instant.now();
        RfidTapEventRequest body = new RfidTapEventRequest(deviceId, "card-999", GateType.SCHOOL, timestamp);

        var first = postTap(deviceId, rawKey, body);
        assertStatus(first, HttpStatus.ACCEPTED);
        assertThat(first.getBody().data().stored()).isTrue();

        // Same device, same (deviceId, timestamp) natural key - simulates a flaky-hardware retry.
        var retry = postTap(deviceId, rawKey, body);
        assertStatus(retry, HttpStatus.ACCEPTED);
        assertThat(retry.getBody().data().stored()).isFalse();

        assertThat(tapEventRepository.countByDeviceId(deviceId)).isEqualTo(1);
    }

    @Test
    void telemetrySampleIsAcceptedAndStored() {
        String deviceId = unique("hw-telemetry-device-");
        String rawKey = provisionDevice(deviceId, "raw-key-" + deviceId);
        Instant timestamp = Instant.now();

        var response = exchangeWithHeaders("/hardware/telemetry", HttpMethod.POST,
                new VehicleTelemetrySampleRequest(deviceId, 42.5, true, false, timestamp), null,
                Map.of("X-Device-Id", deviceId, "X-Device-Key", rawKey),
                new ParameterizedTypeReference<ApiResponse<IngestResultResponse>>() {});

        assertStatus(response, HttpStatus.ACCEPTED);
        assertThat(response.getBody().data().stored()).isTrue();
        assertThat(telemetrySampleRepository.existsByDeviceIdAndSampleTimestamp(deviceId, timestamp)).isTrue();
    }

    @Test
    void deviceIdMismatchBetweenHeaderAndBodyIsRejected() {
        String deviceId = unique("hw-tap-device-");
        String otherDeviceId = unique("hw-tap-device-other-");
        String rawKey = provisionDevice(deviceId, "raw-key-" + deviceId);

        var response = postTap(deviceId, rawKey,
                new RfidTapEventRequest(otherDeviceId, "card-123", GateType.HOME, Instant.now()));

        assertStatus(response, HttpStatus.FORBIDDEN);
    }
}
