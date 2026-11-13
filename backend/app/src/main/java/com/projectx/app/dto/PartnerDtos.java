package com.projectx.app.dto;

import com.projectx.app.domain.Partner;
import com.projectx.app.domain.PartnerStatus;
import com.projectx.app.domain.PartnerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class PartnerDtos {

    private PartnerDtos() {
    }

    /** {@code status} is never accepted here - it is always {@link PartnerStatus#COMING_SOON} (F1). */
    public record PartnerRequest(
            @NotNull PartnerType type, @NotBlank @Size(max = 200) String name,
            @Email @Size(max = 255) String contactEmail, @Size(max = 32) String contactPhone, @Size(max = 1000) String notes) {
    }

    public record PartnerResponse(Long id, PartnerType type, String name, String contactEmail, String contactPhone,
                                  String notes, PartnerStatus status, Instant createdAt) {
        public static PartnerResponse from(Partner p) {
            return new PartnerResponse(p.getId(), p.getType(), p.getName(), p.getContactEmail(), p.getContactPhone(),
                    p.getNotes(), p.getStatus(), p.getCreatedAt());
        }
    }
}
