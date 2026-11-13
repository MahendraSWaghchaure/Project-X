package com.projectx.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class ConsentDtos {

    private ConsentDtos() {
    }

    /** NOT_REQUESTED -> SENT (link out, unexpired) -> EXPIRED | GRANTED | DECLINED. GRANTED also covers consent recorded without a link. */
    public enum ConsentStatus {
        NOT_REQUESTED,
        SENT,
        EXPIRED,
        GRANTED,
        DECLINED
    }

    /**
     * Result of sending a consent link. {@code sentTo} is the masked guardian phone. {@code devLink} is non-null ONLY when
     * {@code projectx.consent.expose-dev-link=true} (local development / tests, where no SMS provider is configured); in
     * production it is always null - a school must never see the link, or it could answer on the guardian's behalf.
     */
    public record ConsentLinkResponse(Long studentId, ConsentStatus consentStatus,
                                      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt, String sentTo, String devLink) {
    }

    /**
     * Public page data. Deliberately minimal: the student's FIRST name and the school name, nothing about the guardian,
     * class, roll number or any other student.
     */
    public record ConsentView(String studentFirstName, String schoolName, @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt) {
    }

    public record ConsentRespondRequest(@NotNull Boolean consent) {
    }

    /** Public answer to a response: the recorded decision, no other data. */
    public record ConsentReceipt(boolean consent) {
    }
}
