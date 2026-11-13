package com.projectx.hardware.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectx.common.web.ApiError;
import com.projectx.common.web.ApiResponse;
import com.projectx.hardware.domain.DeviceApiKey;
import com.projectx.hardware.repo.DeviceApiKeyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Authenticates every {@code /hardware/**} request by a per-device API key, deliberately NOT the
 * JWT stack module-identity's SecurityConfig wires up for everything else - a tap/telemetry device
 * has no user login, so there is no human session to issue a JWT to. See
 * {@code HardwareIngestController}'s javadoc for the full picture; module-identity's
 * {@code SecurityConfig.PUBLIC_ENDPOINTS} has one added entry ({@code /hardware/**}, permitAll in
 * the Spring Security chain) so requests reach this interceptor at all - authentication and
 * rejection from here on is entirely this module's own, same arrangement
 * {@code TrackingIngestController} already uses for its own non-JWT caller (Traccar).
 *
 * <p>Expects two headers: {@code X-Device-Id} and {@code X-Device-Key} (the raw key - only its
 * SHA-256 is ever stored, see DeviceKeyHasher). On success, stashes the authenticated device id as
 * a request attribute so the controller/service layer can cross-check it against whatever
 * {@code deviceId} the request body itself claims, without this interceptor having to parse the
 * body (which would consume the stream before Spring MVC's own {@code @RequestBody} binding gets
 * to it).
 */
@Component
public class DeviceApiKeyAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DeviceApiKeyAuthInterceptor.class);
    public static final String AUTHENTICATED_DEVICE_ID_ATTRIBUTE = "projectx.hardware.authenticatedDeviceId";

    private final DeviceApiKeyRepository deviceApiKeyRepository;
    private final HardwareRateLimiter rateLimiter;
    private final HardwareRateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    public DeviceApiKeyAuthInterceptor(DeviceApiKeyRepository deviceApiKeyRepository, HardwareRateLimiter rateLimiter,
                                        HardwareRateLimitProperties rateLimitProperties, ObjectMapper objectMapper) {
        this.deviceApiKeyRepository = deviceApiKeyRepository;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String deviceId = request.getHeader("X-Device-Id");
        String rawKey = request.getHeader("X-Device-Key");
        if (deviceId == null || deviceId.isBlank() || rawKey == null || rawKey.isBlank()) {
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "DEVICE_AUTH_MISSING",
                    "X-Device-Id and X-Device-Key headers are required");
        }

        Optional<DeviceApiKey> key = deviceApiKeyRepository.findByDeviceIdAndActiveTrue(deviceId);
        if (key.isEmpty() || !DeviceKeyHasher.matches(rawKey, key.get().getKeyHash())) {
            log.warn("Rejected hardware ingestion call: invalid device key for deviceId={}", deviceId);
            return reject(response, HttpServletResponse.SC_UNAUTHORIZED, "DEVICE_AUTH_INVALID", "Invalid device id or key");
        }

        int limitPerMinute = request.getRequestURI().endsWith("/telemetry")
                ? rateLimitProperties.getTelemetryPerMinute() : rateLimitProperties.getTapPerMinute();
        if (!rateLimiter.allow(request.getRequestURI(), deviceId, limitPerMinute)) {
            return reject(response, 429, "RATE_LIMITED", "Too many requests from this device - please wait a minute and try again");
        }

        request.setAttribute(AUTHENTICATED_DEVICE_ID_ATTRIBUTE, deviceId);
        return true;
    }

    private boolean reject(HttpServletResponse response, int status, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(ApiError.of(code, message))));
        return false;
    }
}
