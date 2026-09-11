package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.SchoolResponse;
import com.projectx.school.dto.UpdateSchoolRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/**
 * Proves @Version actually prevents a lost update under real concurrent writes - not just that
 * the annotation is present. Fires several genuinely concurrent PATCH requests (a real
 * ExecutorService + CountDownLatch starting gate, not a loop) at the same School row and checks
 * the two things that matter: nothing 500s (GlobalExceptionHandler's
 * ObjectOptimisticLockingFailureException mapping actually catches it), and the row ends up
 * holding exactly one of the attempted values, never a torn/merged state.
 */
class OptimisticLockingConcurrencyTest extends IntegrationTestBase {

    private static final int CONCURRENT_REQUESTS = 8;

    @Test
    void concurrentSchoolUpdatesNeverSilentlyLoseAWriteOrCorruptState() throws Exception {
        String platformToken = platformAdminToken();

        var createRequest = new CreateSchoolRequest(unique("Concurrency School "), "1 Test Rd", "Pune", null, null,
                null, null, null, null, null, null, "Admin", "User", unique("schooladmin") + "@test.example", "password123");
        ResponseEntity<ApiResponse<SchoolResponse>> createResponse = exchange(
                "/schools", HttpMethod.POST, createRequest, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(createResponse);
        Long schoolId = createResponse.getBody().data().id();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<HttpStatusCode>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            String attemptedName = "Concurrent Name " + i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                go.await();
                var updateRequest = new UpdateSchoolRequest(attemptedName, "1 Test Rd", "Pune", null, null, null,
                        null, null, null, null, null);
                ResponseEntity<ApiResponse<SchoolResponse>> response = exchange(
                        "/schools/" + schoolId, HttpMethod.PATCH, updateRequest, platformToken, new ParameterizedTypeReference<ApiResponse<SchoolResponse>>() {});
                return response.getStatusCode();
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).as("all threads reached the starting gate").isTrue();
        go.countDown();

        List<HttpStatusCode> statuses = new ArrayList<>();
        for (Future<HttpStatusCode> future : futures) {
            statuses.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        assertThat(statuses)
                .as("every concurrent write either succeeds or is cleanly rejected as a conflict - never a 500")
                .allMatch(status -> status == HttpStatus.OK || status == HttpStatus.CONFLICT);
        assertThat(statuses)
                .as("with 8 genuinely concurrent writers on one row, at least one must lose the optimistic-lock race")
                .contains(HttpStatus.CONFLICT);

        ResponseEntity<ApiResponse<SchoolResponse>> finalState = exchange(
                "/schools/" + schoolId, HttpMethod.GET, null, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(finalState);
        assertThat(finalState.getBody().data().name())
                .as("the row holds exactly one attempted value, not a torn/corrupted mix of concurrent writes")
                .matches("Concurrent Name \\d");
    }
}
