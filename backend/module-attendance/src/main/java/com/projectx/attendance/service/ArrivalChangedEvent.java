package com.projectx.attendance.service;

import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;

/**
 * Module-internal: published inside the transaction that changed an Arrival, consumed AFTER_COMMIT
 * (so a rolled-back change never pushes a phantom alert) by ArrivalChangedListener, which hands
 * the fan-out (STOMP + notifications) to the async pool.
 *
 * @param previousOutcome null when the arrival was just opened.
 */
public record ArrivalChangedEvent(ArrivalResponse arrival, ArrivalOutcome previousOutcome) {
}
