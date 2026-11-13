package com.projectx.notification.dto;

import com.projectx.notification.domain.Inquiry;
import com.projectx.notification.domain.InquiryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class InquiryDtos {

    private InquiryDtos() {
    }

    /**
     * Public form body. {@code website} is a honeypot: real visitors never see or fill it, so a non-empty value is
     * silently accepted and dropped (bots get the same success response and learn nothing).
     */
    public record CreateInquiryRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(min = 5, max = 150) String contact,
            @NotBlank @Size(min = 5, max = 2000) String message,
            @Size(max = 200) String website) {
    }

    /** Deliberately says nothing about the stored row: no id, no echo of the visitor's data. */
    public record InquiryReceipt(boolean received) {
    }

    public record InquiryResponse(Long id, String name, String contact, String message, InquiryStatus status,
                                  Instant createdAt, Instant handledAt) {
        public static InquiryResponse from(Inquiry i) {
            return new InquiryResponse(i.getId(), i.getName(), i.getContact(), i.getMessage(), i.getStatus(),
                    i.getCreatedAt(), i.getHandledAt());
        }
    }

    public record UpdateInquiryStatusRequest(@NotNull InquiryStatus status) {
    }
}
