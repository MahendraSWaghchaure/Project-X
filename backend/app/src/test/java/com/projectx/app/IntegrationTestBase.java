package com.projectx.app;

import com.projectx.common.web.ApiResponse;
import com.projectx.identity.dto.LoginRequest;
import com.projectx.identity.dto.TokenResponse;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

/**
 * Full-stack HTTP integration tests: every call goes over real HTTP to a real Spring context
 * (JWT auth, Flyway-migrated schema, Redis rate limiting, the works), the same way this whole
 * app was hand-verified with curl throughout development - just automated and repeatable now.
 * Runs against a dedicated projectx_test Postgres database (see application-test.yml), not the
 * dev database, but on the same local Postgres+PostGIS instance since Testcontainers/Docker
 * aren't available in this environment.
 *
 * <p>Uses a plain RestTemplate rather than Spring Boot's usual TestRestTemplate - Boot 4.0.0
 * removed TestRestTemplate from spring-boot-test entirely (only @LocalServerPort survived, moved
 * to org.springframework.boot.test.web.server), so this builds absolute localhost:{port} URLs by
 * hand instead. A plain RestTemplate also means 4xx/5xx responses throw
 * HttpStatusCodeException rather than returning a normal ResponseEntity - errorHandler below
 * disables that so assertStatus()/assertSuccess() can inspect the body either way, exactly like
 * TestRestTemplate's default behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String PLATFORM_ADMIN_EMAIL = "admin@projectx.local";
    protected static final String PLATFORM_ADMIN_PASSWORD = "changeme123";

    @LocalServerPort
    protected int port;

    // The JDK's default HttpURLConnection-based request factory can't send PATCH at all ("Invalid
    // HTTP method: PATCH" - a decades-old HttpURLConnection limitation) and is unreliable reading
    // response bodies on non-2xx status. JdkClientHttpRequestFactory wraps java.net.http.HttpClient
    // (built into the JDK since 11) instead, which has neither problem.
    private final RestTemplate restTemplateInstance = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        RestTemplate template = new RestTemplate(new org.springframework.http.client.JdkClientHttpRequestFactory());
        // Never throw on non-2xx - every assertion here reads status/body directly instead.
        template.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public void handleError(java.net.URI url, org.springframework.http.HttpMethod method,
                                     org.springframework.http.client.ClientHttpResponse response) {
                // no-op: swallow so exchange() always returns a normal ResponseEntity
            }
        });
        return template;
    }

    private RestTemplate restTemplate() {
        return restTemplateInstance;
    }

    /** Every test that needs unique data (emails, license/registration numbers) suffixes with this - tests share one long-lived database. */
    protected String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String login(String email, String password) {
        ResponseEntity<ApiResponse<TokenResponse>> response = exchange(
                "/auth/login", HttpMethod.POST, new LoginRequest(email, password), null,
                new ParameterizedTypeReference<>() {});
        assertSuccess(response);
        return response.getBody().data().accessToken();
    }

    protected String platformAdminToken() {
        return login(PLATFORM_ADMIN_EMAIL, PLATFORM_ADMIN_PASSWORD);
    }

    protected <T> ResponseEntity<ApiResponse<T>> exchange(String path, HttpMethod method, Object body, String bearerToken,
                                                            ParameterizedTypeReference<ApiResponse<T>> responseType) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate().exchange("http://localhost:" + port + path, method, entity, responseType);
    }

    protected <T> void assertSuccess(ResponseEntity<ApiResponse<T>> response) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || !response.getBody().success()) {
            throw new AssertionError("Expected success but got " + response.getStatusCode() + ": "
                    + (response.getBody() != null ? response.getBody().error() : "null body"));
        }
    }

    protected void assertStatus(ResponseEntity<?> response, HttpStatus expected) {
        if (response.getStatusCode() != expected) {
            throw new AssertionError("Expected " + expected + " but got " + response.getStatusCode() + ": " + response.getBody());
        }
    }
}
