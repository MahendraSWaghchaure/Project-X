package com.projectx.hardware.web;

import com.projectx.common.web.ApiError;
import com.projectx.common.web.ApiResponse;
import com.projectx.hardware.dto.IngestResultResponse;
import com.projectx.hardware.dto.RfidTapEventRequest;
import com.projectx.hardware.dto.VehicleTelemetrySampleRequest;
import com.projectx.hardware.security.DeviceApiKeyAuthInterceptor;
import com.projectx.hardware.service.HardwareIngestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Raw ingestion for hardware that does not exist yet in this deployment - a real RFID/NFC gate
 * reader and a real in-vehicle telemetry box are not wired to anything today; this is the
 * endpoint shape they will POST to once they exist, built now so the application side of that
 * integration isn't a blocker when real hardware shows up.
 *
 * <p><b>Deliberately not wired into any safety-critical decision path.</b> A tap here does NOT
 * create an {@code Arrival} (module-attendance) or mark attendance, and a telemetry sample here
 * does NOT feed module-tracking's existing GPS-ping-driven speed-limit/harsh-braking safety checks
 * (see {@code ARCHITECTURE.md} item 1 and module-attendance's arrival pipeline for the real,
 * already-wired paths). This module only authenticates the device, rate-limits it, and stores the
 * raw sample idempotently - turning it into a safety signal is a deliberate, separate decision for
 * whoever integrates real hardware, not something this pass does on their behalf.
 *
 * <p>Authentication is a per-device API key (see DeviceApiKeyAuthInterceptor), never a user JWT -
 * hardware has no user login. {@code /hardware/**} is listed in module-identity's
 * {@code SecurityConfig.PUBLIC_ENDPOINTS} (permitAll at the Spring Security layer, same as
 * {@code /tracking/ingest/**}) specifically so requests reach this interceptor instead of being
 * rejected for lacking a Bearer JWT.
 */
@RestController
@RequestMapping("/hardware")
public class HardwareIngestController {

    private final HardwareIngestService ingestService;

    public HardwareIngestController(HardwareIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/tap")
    public ResponseEntity<ApiResponse<IngestResultResponse>> tap(@Valid @RequestBody RfidTapEventRequest request,
                                                                   HttpServletRequest httpRequest) {
        ResponseEntity<ApiResponse<IngestResultResponse>> mismatch = rejectIfDeviceIdMismatch(request.deviceId(), httpRequest);
        if (mismatch != null) {
            return mismatch;
        }
        boolean stored = ingestService.recordTap(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(new IngestResultResponse(stored)));
    }

    @PostMapping("/telemetry")
    public ResponseEntity<ApiResponse<IngestResultResponse>> telemetry(@Valid @RequestBody VehicleTelemetrySampleRequest request,
                                                                         HttpServletRequest httpRequest) {
        ResponseEntity<ApiResponse<IngestResultResponse>> mismatch = rejectIfDeviceIdMismatch(request.deviceId(), httpRequest);
        if (mismatch != null) {
            return mismatch;
        }
        boolean stored = ingestService.recordTelemetry(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(new IngestResultResponse(stored)));
    }

    /** The device authenticated as one id (X-Device-Id, checked by the interceptor) - it should never be reporting events for a different one. */
    private ResponseEntity<ApiResponse<IngestResultResponse>> rejectIfDeviceIdMismatch(String bodyDeviceId, HttpServletRequest httpRequest) {
        Object authenticatedDeviceId = httpRequest.getAttribute(DeviceApiKeyAuthInterceptor.AUTHENTICATED_DEVICE_ID_ATTRIBUTE);
        if (authenticatedDeviceId == null || !authenticatedDeviceId.equals(bodyDeviceId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
                    ApiError.of("DEVICE_ID_MISMATCH", "Request body deviceId does not match the authenticated device")));
        }
        return null;
    }
}
