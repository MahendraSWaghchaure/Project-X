package com.projectx.hardware.dto;

import com.projectx.hardware.domain.GateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RfidTapEventRequest(
        @NotBlank String deviceId,
        @NotBlank String cardToken,
        @NotNull GateType gateType,
        @NotNull Instant timestamp) {
}
