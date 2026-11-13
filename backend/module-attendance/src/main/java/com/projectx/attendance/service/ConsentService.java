package com.projectx.attendance.service;

import com.projectx.attendance.domain.ConsentToken;
import com.projectx.attendance.domain.Student;
import com.projectx.attendance.dto.ConsentDtos.ConsentLinkResponse;
import com.projectx.attendance.dto.ConsentDtos.ConsentReceipt;
import com.projectx.attendance.dto.ConsentDtos.ConsentStatus;
import com.projectx.attendance.dto.ConsentDtos.ConsentView;
import com.projectx.attendance.repo.ConsentTokenRepository;
import com.projectx.attendance.repo.StudentRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.GoneException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.trace.TraceIds;
import com.projectx.identity.security.TokenHasher;
import com.projectx.notification.service.NotificationDispatchService;
import com.projectx.school.repo.SchoolRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guardian consent by SMS link (DPDP: verifiable consent for a minor's contact to be used for promotion).
 * <ul>
 *   <li>The link carries a 256-bit random token; only its SHA-256 is stored. Valid 48 hours, single use.</li>
 *   <li>Opening the link (GET) changes nothing - link-preview bots cannot consume it; only an explicit POST answer does.</li>
 *   <li>Answering is one atomic conditional UPDATE ({@link ConsentTokenRepository#markUsed}) so two concurrent answers
 *       cannot both win; the loser gets the same 410 as an expired link.</li>
 *   <li>Every failure on the public side (unknown, expired, used) is the same generic 410 - no oracle for token guessing,
 *       and no student data beyond the first name is ever returned.</li>
 *   <li>The SMS body (which contains the token) is never written to notification_records.</li>
 *   <li>A school never sees the link in the API response (it could consent on the guardian's behalf) - see
 *       {@code projectx.consent.expose-dev-link} for the dev/test-only exception.</li>
 * </ul>
 */
@Service
@Transactional
public class ConsentService {

    public static final Duration TOKEN_TTL = Duration.ofHours(48);
    /** SMS-cost / harassment guard: at most this many links per student per rolling 24 hours. */
    static final int MAX_LINKS_PER_DAY = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String GONE_MESSAGE = "This link is no longer valid";

    private final ConsentTokenRepository tokenRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final NotificationDispatchService dispatchService;
    private final ApplicationEventPublisher eventPublisher;
    private final String frontendBaseUrl;
    private final boolean exposeDevLink;

    public ConsentService(ConsentTokenRepository tokenRepository, StudentRepository studentRepository,
                          SchoolRepository schoolRepository, NotificationDispatchService dispatchService,
                          ApplicationEventPublisher eventPublisher,
                          @Value("${projectx.frontend-base-url:http://localhost:5173}") String frontendBaseUrl,
                          @Value("${projectx.consent.expose-dev-link:false}") boolean exposeDevLink) {
        this.tokenRepository = tokenRepository;
        this.studentRepository = studentRepository;
        this.schoolRepository = schoolRepository;
        this.dispatchService = dispatchService;
        this.eventPublisher = eventPublisher;
        this.frontendBaseUrl = frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
        this.exposeDevLink = exposeDevLink;
    }

    // ------------------------------------------------------------------ school / platform admin side

    public ConsentLinkResponse requestLink(Long schoolId, Long studentId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));
        Instant now = Instant.now();
        if (tokenRepository.countByStudentIdAndCreatedAtAfter(studentId, now.minus(Duration.ofDays(1))) >= MAX_LINKS_PER_DAY) {
            throw new ConflictException("Too many consent links were sent for this student today - try again tomorrow");
        }
        tokenRepository.expireOutstanding(studentId, now);

        String rawToken = newToken();
        ConsentToken token = tokenRepository.save(new ConsentToken(studentId, schoolId, TokenHasher.sha256(rawToken),
                now.plus(TOKEN_TTL), CurrentUserContext.get().userId()));

        String schoolName = schoolRepository.findById(schoolId).map(s -> s.getName()).orElse("Your child's school");
        String link = frontendBaseUrl + "/consent/" + rawToken;
        String text = "Sunju: " + schoolName + " asks if you agree to receive information about school transport for "
                + student.getFirstName() + ". Please answer within 48 hours: ";
        // Inside the transaction; the dispatch itself is @Async and only runs the SMS - the link never reaches the log row.
        dispatchService.dispatchSmsToNumber(student.getGuardianPhone(), "consent.LINK", text + link, text + "[link]");

        eventPublisher.publishEvent(AuditEvent.of("CONSENT_LINK_SENT", "STUDENT", studentId, schoolId,
                "expires=" + token.getExpiresAt()));
        return new ConsentLinkResponse(studentId, ConsentStatus.SENT, token.getExpiresAt(),
                mask(student.getGuardianPhone()), exposeDevLink ? link : null);
    }

    /** Consent status of a page of students from their newest link (one query) and the stored flag. */
    @Transactional(readOnly = true)
    public Map<Long, ConsentInfo> statusFor(Collection<Student> students) {
        Map<Long, ConsentInfo> out = new HashMap<>();
        if (students.isEmpty()) {
            return out;
        }
        Map<Long, ConsentToken> latest = new HashMap<>();
        for (ConsentToken t : tokenRepository.findLatestForStudents(students.stream().map(Student::getId).toList())) {
            latest.put(t.getStudentId(), t);
        }
        Instant now = Instant.now();
        for (Student s : students) {
            out.put(s.getId(), statusOf(s, latest.get(s.getId()), now));
        }
        return out;
    }

    public record ConsentInfo(ConsentStatus status, Instant expiresAt) {
    }

    static ConsentInfo statusOf(Student student, ConsentToken latest, Instant now) {
        if (student.isMarketingConsent()) {
            return new ConsentInfo(ConsentStatus.GRANTED, null);
        }
        if (latest == null) {
            return new ConsentInfo(ConsentStatus.NOT_REQUESTED, null);
        }
        if (latest.getUsedAt() != null) {
            // Granted-then-revoked (admin switched it off afterwards) is "not requested" again, not "declined".
            return Boolean.FALSE.equals(latest.getResponse())
                    ? new ConsentInfo(ConsentStatus.DECLINED, null)
                    : new ConsentInfo(ConsentStatus.NOT_REQUESTED, null);
        }
        return latest.getExpiresAt().isAfter(now)
                ? new ConsentInfo(ConsentStatus.SENT, latest.getExpiresAt())
                : new ConsentInfo(ConsentStatus.EXPIRED, null);
    }

    // ------------------------------------------------------------------ public side (no authentication)

    @Transactional(readOnly = true)
    public ConsentView view(String rawToken) {
        ConsentToken token = openToken(rawToken);
        Student student = studentRepository.findById(token.getStudentId()).orElseThrow(() -> new GoneException(GONE_MESSAGE));
        String schoolName = schoolRepository.findById(token.getSchoolId()).map(s -> s.getName()).orElse("");
        return new ConsentView(student.getFirstName(), schoolName, token.getExpiresAt());
    }

    public ConsentReceipt respond(String rawToken, boolean consent) {
        String hash = hashOrGone(rawToken);
        Instant now = Instant.now();
        // Atomic single-use gate: exactly one concurrent caller gets 1 here.
        if (tokenRepository.markUsed(hash, consent, now) != 1) {
            throw new GoneException(GONE_MESSAGE);
        }
        ConsentToken token = tokenRepository.findByTokenHash(hash).orElseThrow(() -> new GoneException(GONE_MESSAGE));
        studentRepository.applyConsent(token.getStudentId(), consent, now, "SMS_LINK");
        eventPublisher.publishEvent(new AuditEvent(null, "guardian-sms-link", "GUARDIAN", "STUDENT_CONSENT_RESPONDED", "STUDENT",
                token.getStudentId(), token.getSchoolId(), "consent=" + consent + " via SMS link", now, MDC.get(TraceIds.MDC_KEY)));
        return new ConsentReceipt(consent);
    }

    // ------------------------------------------------------------------ helpers

    private ConsentToken openToken(String rawToken) {
        ConsentToken token = tokenRepository.findByTokenHash(hashOrGone(rawToken)).orElseThrow(() -> new GoneException(GONE_MESSAGE));
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(Instant.now())) {
            throw new GoneException(GONE_MESSAGE);
        }
        return token;
    }

    private static String hashOrGone(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) {
            throw new GoneException(GONE_MESSAGE);
        }
        return TokenHasher.sha256(rawToken);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
