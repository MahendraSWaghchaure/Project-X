package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * The public (unauthenticated) endpoints are rate limited per client IP by RateLimitFilter. Each test uses its own
 * X-Forwarded-For value so it has a private bucket in the shared Redis - this still works because the test client
 * connects from loopback, which stays in RateLimitProperties' trustedProxies default specifically so local dev/test
 * runs are unaffected. The filter no longer trusts X-Forwarded-For from anything else (a real client spoofing it
 * directly, or an unconfigured/different reverse proxy, now gets rate-limited by their own real address instead of
 * bypassing the limit with a fresh header value every request); limits are lowered for this context only.
 */
@TestPropertySource(properties = {
        "projectx.security.rate-limit.inquiry-per-minute=3",
        "projectx.security.rate-limit.consent-view-per-minute=4",
        "projectx.security.rate-limit.consent-respond-per-minute=2"})
class Phase2RateLimitTest extends IntegrationTestBase {

    private static Map<String, String> ip() {
        return Map.of("X-Forwarded-For", "10." + (int) (Math.random() * 200) + "." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
    }

    @Test
    void inquiriesAreLimitedPerClient() {
        Map<String, String> client = ip();
        Map<String, String> body = Map.of("name", "Rate Tester", "contact", "rate@example.com", "message", "Hello, please call me back.");
        for (int i = 0; i < 3; i++) {
            ResponseEntity<ApiResponse<Map<String, Object>>> r = exchangeWithHeaders("/public/inquiries", HttpMethod.POST, body, null, client,
                    new ParameterizedTypeReference<>() {});
            assertStatus(r, HttpStatus.CREATED);
        }
        ResponseEntity<ApiResponse<Map<String, Object>>> limited = exchangeWithHeaders("/public/inquiries", HttpMethod.POST, body, null, client,
                new ParameterizedTypeReference<>() {});
        assertStatus(limited, HttpStatus.TOO_MANY_REQUESTS);
        assertThat(limited.getBody().error().code()).isEqualTo("RATE_LIMITED");
        // A different client is unaffected.
        assertStatus(this.<Map<String, Object>>exchangeWithHeaders("/public/inquiries", HttpMethod.POST, body, null, ip(),
                new ParameterizedTypeReference<>() {}), HttpStatus.CREATED);
    }

    @Test
    void consentViewAndRespondAreLimitedPerClientAndSeparately() {
        Map<String, String> client = ip();
        String token = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 4; i++) {
            assertStatus(this.<Map<String, Object>>exchangeWithHeaders("/public/consent/" + token, HttpMethod.GET, null, null, client,
                    new ParameterizedTypeReference<>() {}), HttpStatus.GONE);
        }
        assertStatus(this.<Map<String, Object>>exchangeWithHeaders("/public/consent/" + token, HttpMethod.GET, null, null, client,
                new ParameterizedTypeReference<>() {}), HttpStatus.TOO_MANY_REQUESTS);
        // Respond has its own (tighter) bucket, still untouched for this client.
        for (int i = 0; i < 2; i++) {
            assertStatus(this.<Map<String, Object>>exchangeWithHeaders("/public/consent/" + token + "/respond", HttpMethod.POST, Map.of("consent", true),
                    null, client, new ParameterizedTypeReference<>() {}), HttpStatus.GONE);
        }
        assertStatus(this.<Map<String, Object>>exchangeWithHeaders("/public/consent/" + token + "/respond", HttpMethod.POST, Map.of("consent", true),
                null, client, new ParameterizedTypeReference<>() {}), HttpStatus.TOO_MANY_REQUESTS);
    }
}
