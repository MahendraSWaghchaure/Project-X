package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.attendance.dto.ConsentDtos.ConsentLinkResponse;
import com.projectx.attendance.dto.ConsentDtos.ConsentStatus;
import com.projectx.attendance.dto.StaffDtos.LeadResponse;
import com.projectx.attendance.dto.StudentDtos.StudentRequest;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A2: guardian consent by SMS link - 48 h expiry, single use (also under concurrency), uniform 410, no PII on the public side,
 * token never stored raw, provenance recorded, only admins of the student's school can send one.
 */
class Phase2ConsentTest extends SchoolAttendanceTestKit {

    @Autowired
    JdbcTemplate jdbc;

    private StudentResponse student(Env env, String first) {
        return ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students", HttpMethod.POST,
                new StudentRequest(env.classroomId(), null, first, "Child", null, null, "Parent of " + first, "9876543210", null, false, null, null),
                env.adminToken(), new ParameterizedTypeReference<>() {}));
    }

    private StudentResponse reload(Env env, Long id) {
        return ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + id, HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
    }

    private ConsentLinkResponse sendLink(Env env, Long studentId) {
        ResponseEntity<ApiResponse<ConsentLinkResponse>> r = exchange("/schools/" + env.schoolId() + "/students/" + studentId + "/consent-link",
                HttpMethod.POST, null, env.adminToken(), new ParameterizedTypeReference<>() {});
        assertStatus(r, HttpStatus.CREATED);
        return r.getBody().data();
    }

    private static String tokenOf(ConsentLinkResponse link) {
        return link.devLink().substring(link.devLink().lastIndexOf("/consent/") + "/consent/".length());
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> view(String token) {
        return exchange("/public/consent/" + token, HttpMethod.GET, null, null, new ParameterizedTypeReference<>() {});
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> respond(String token, boolean consent) {
        return exchange("/public/consent/" + token + "/respond", HttpMethod.POST, Map.of("consent", consent), null, new ParameterizedTypeReference<>() {});
    }

    @Test
    void grantFlowIsSingleUseHashedAtRestAndRecordsProvenance() {
        Env env = buildEnv();
        StudentResponse s = student(env, "Granting");
        assertThat(s.consentStatus()).isEqualTo(ConsentStatus.NOT_REQUESTED);
        assertThat(s.marketingConsent()).isFalse();

        ConsentLinkResponse link = sendLink(env, s.id());
        assertThat(link.consentStatus()).isEqualTo(ConsentStatus.SENT);
        assertThat(link.sentTo()).endsWith("3210").doesNotContain("98765");
        assertThat(Duration.between(Instant.now(), link.expiresAt())).isBetween(Duration.ofHours(47).plusMinutes(55), Duration.ofHours(48).plusMinutes(1));
        String token = tokenOf(link);
        assertThat(token.length()).isGreaterThanOrEqualTo(40);

        StudentResponse sent = reload(env, s.id());
        assertThat(sent.consentStatus()).isEqualTo(ConsentStatus.SENT);
        assertThat(sent.consentExpiresAt()).isNotNull();
        assertThat(sent.marketingConsent()).isFalse();

        // Only a hash is stored; the SMS log row never contains the link (wait for the async dispatch to have logged it).
        assertThat(jdbc.queryForObject("SELECT count(*) FROM consent_tokens WHERE token_hash = ?", Integer.class, token)).isZero();
        awaitTrue(() -> jdbc.queryForObject("SELECT count(*) FROM notification_records WHERE template_key = 'consent.LINK' AND recipient_contact LIKE '%3210'",
                Integer.class) > 0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_records WHERE body LIKE ?", Integer.class, "%" + token + "%")).isZero();
        assertThat(jdbc.queryForObject("SELECT recipient_user_id FROM notification_records WHERE template_key = 'consent.LINK' ORDER BY id DESC LIMIT 1",
                Long.class)).isNull();

        // Public view: minimal data, nothing else, and it does NOT consume the link.
        ResponseEntity<ApiResponse<Map<String, Object>>> viewed = view(token);
        assertStatus(viewed, HttpStatus.OK);
        assertThat(viewed.getBody().data().keySet()).containsExactlyInAnyOrder("studentFirstName", "schoolName", "expiresAt");
        assertThat(viewed.getBody().data()).containsEntry("studentFirstName", "Granting");
        assertStatus(view(token), HttpStatus.OK);

        // Respond: consent recorded with provenance, the roster shows GRANTED, and the guardian is now a lead.
        ResponseEntity<ApiResponse<Map<String, Object>>> answered = respond(token, true);
        assertStatus(answered, HttpStatus.OK);
        assertThat(answered.getBody().data()).containsEntry("consent", true).hasSize(1);
        StudentResponse granted = reload(env, s.id());
        assertThat(granted.consentStatus()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(granted.marketingConsent()).isTrue();
        assertThat(granted.consentSource()).isEqualTo("SMS_LINK");
        assertThat(granted.consentAt()).isNotNull();
        PageResponse<LeadResponse> leads = ok(this.<PageResponse<LeadResponse>>exchange("/platform-admin/leads?size=100&page=0", HttpMethod.GET, null,
                platformAdminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(leads.totalElements()).isGreaterThanOrEqualTo(1);
        awaitTrue(() -> jdbc.queryForObject("SELECT count(*) FROM audit_log_entries WHERE action = 'STUDENT_CONSENT_RESPONDED' AND entity_id = ?",
                Integer.class, s.id()) > 0);

        // Single use: every further attempt - same answer or the opposite - and the view are the same generic 410; consent is unchanged.
        ResponseEntity<ApiResponse<Map<String, Object>>> again = respond(token, true);
        assertStatus(again, HttpStatus.GONE);
        assertStatus(respond(token, false), HttpStatus.GONE);
        ResponseEntity<ApiResponse<Map<String, Object>>> viewedAfter = view(token);
        assertStatus(viewedAfter, HttpStatus.GONE);
        assertThat(reload(env, s.id()).marketingConsent()).isTrue();

        // Unknown, malformed and used tokens are indistinguishable (no oracle) and leak nothing.
        ResponseEntity<ApiResponse<Map<String, Object>>> unknown = view(UUID.randomUUID().toString().replace("-", ""));
        assertStatus(unknown, HttpStatus.GONE);
        assertThat(unknown.getBody().error().code()).isEqualTo(again.getBody().error().code()).isEqualTo(viewedAfter.getBody().error().code());
        assertThat(unknown.getBody().error().message()).isEqualTo(again.getBody().error().message());
        assertThat(unknown.getBody().error().message()).doesNotContain("Granting");
        assertStatus(respond("x", true), HttpStatus.GONE);
        // A body without the answer is a plain 400, not a consent.
        assertStatus(this.<Map<String, Object>>exchange("/public/consent/" + token + "/respond", HttpMethod.POST, Map.of(), null,
                new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
    }

    @Test
    void declineExpiryAndSupersededLinks() {
        Env env = buildEnv();

        // Decline: consent stays off and the roster says DECLINED.
        StudentResponse decliner = student(env, "Declining");
        String declineToken = tokenOf(sendLink(env, decliner.id()));
        assertStatus(respond(declineToken, false), HttpStatus.OK);
        StudentResponse declined = reload(env, decliner.id());
        assertThat(declined.marketingConsent()).isFalse();
        assertThat(declined.consentStatus()).isEqualTo(ConsentStatus.DECLINED);

        // Expiry (48 h passed): both view and respond are 410 and the roster says EXPIRED; nothing changed.
        StudentResponse late = student(env, "Late");
        String lateToken = tokenOf(sendLink(env, late.id()));
        jdbc.update("UPDATE consent_tokens SET expires_at = now() - interval '1 minute' WHERE student_id = ?", late.id());
        assertStatus(view(lateToken), HttpStatus.GONE);
        assertStatus(respond(lateToken, true), HttpStatus.GONE);
        StudentResponse expired = reload(env, late.id());
        assertThat(expired.consentStatus()).isEqualTo(ConsentStatus.EXPIRED);
        assertThat(expired.marketingConsent()).isFalse();
        // ...and a fresh link revives the flow.
        String retry = tokenOf(sendLink(env, late.id()));
        assertStatus(respond(retry, true), HttpStatus.OK);
        assertThat(reload(env, late.id()).consentStatus()).isEqualTo(ConsentStatus.GRANTED);

        // Superseded: a newer link kills the older one.
        StudentResponse twice = student(env, "Twice");
        String first = tokenOf(sendLink(env, twice.id()));
        String second = tokenOf(sendLink(env, twice.id()));
        assertThat(first).isNotEqualTo(second);
        assertStatus(respond(first, true), HttpStatus.GONE);
        assertStatus(respond(second, true), HttpStatus.OK);

        // A granted-then-revoked consent (admin switches it off) reads NOT_REQUESTED again, not DECLINED.
        StudentResponse granted = reload(env, twice.id());
        StudentRequest revoke = new StudentRequest(env.classroomId(), null, "Twice", "Child", null, null, "Parent of Twice", "9876543210", null,
                false, null, granted.version());
        StudentResponse revoked = ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + twice.id(), HttpMethod.PUT, revoke,
                env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(revoked.marketingConsent()).isFalse();
        assertThat(revoked.consentStatus()).isEqualTo(ConsentStatus.NOT_REQUESTED);
        assertThat(revoked.consentSource()).isEqualTo("ADMIN_EDIT");
    }

    @Test
    void onlyAdminsOfTheStudentsSchoolCanSendAndAtMostFiveLinksADay() {
        Env env = buildEnv();
        Env other = buildEnv();
        StudentResponse s = student(env, "Guarded");
        String path = "/schools/" + env.schoolId() + "/students/" + s.id() + "/consent-link";

        for (String denied : List.of(env.teacherToken(), env.parentToken(), env.driverToken(), other.adminToken(), other.teacherToken())) {
            assertStatus(this.<ConsentLinkResponse>exchange(path, HttpMethod.POST, null, denied, new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        }
        assertStatus(this.<ConsentLinkResponse>exchange(path, HttpMethod.POST, null, null, new ParameterizedTypeReference<>() {}), HttpStatus.UNAUTHORIZED);
        // The platform admin can; a student id of another school behind my school id is a 404.
        assertStatus(this.<ConsentLinkResponse>exchange(path, HttpMethod.POST, null, platformAdminToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.CREATED);
        StudentResponse foreign = student(other, "Foreign");
        assertStatus(this.<ConsentLinkResponse>exchange("/schools/" + env.schoolId() + "/students/" + foreign.id() + "/consent-link", HttpMethod.POST,
                null, env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);

        for (int i = 0; i < 4; i++) {
            sendLink(env, s.id());
        }
        assertStatus(this.<ConsentLinkResponse>exchange(path, HttpMethod.POST, null, env.adminToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.CONFLICT);
    }

    @Test
    void concurrentAnswersOnOneLinkProduceExactlyOneWinner() throws Exception {
        Env env = buildEnv();
        StudentResponse s = student(env, "Racing");
        String token = tokenOf(sendLink(env, s.id()));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<HttpStatus>> calls = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                boolean answer = i % 2 == 0;
                calls.add(() -> HttpStatus.valueOf(respond(token, answer).getStatusCode().value()));
            }
            List<HttpStatus> results = new ArrayList<>();
            for (Future<HttpStatus> f : pool.invokeAll(calls)) {
                results.add(f.get());
            }
            assertThat(results.stream().filter(r -> r == HttpStatus.OK).count()).as("winners: %s", results).isEqualTo(1);
            assertThat(results.stream().filter(r -> r == HttpStatus.GONE).count()).isEqualTo(7);
        } finally {
            pool.shutdownNow();
        }
        // The stored decision is the winner's.
        Boolean stored = jdbc.queryForObject("SELECT response FROM consent_tokens WHERE student_id = ?", Boolean.class, s.id());
        assertThat(reload(env, s.id()).marketingConsent()).isEqualTo(stored);
    }

    private static void awaitTrue(java.util.function.BooleanSupplier condition) {
        for (int i = 0; i < 50; i++) {
            if (condition.getAsBoolean()) {
                return;
            }
            sleep(200);
        }
        throw new AssertionError("condition never became true");
    }
}
