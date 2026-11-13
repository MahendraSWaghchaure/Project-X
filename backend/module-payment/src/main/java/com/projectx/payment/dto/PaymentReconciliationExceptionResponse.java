package com.projectx.payment.dto;

import com.projectx.payment.domain.PaymentReconciliationException;
import com.projectx.payment.domain.ReconciliationEntityType;
import com.projectx.payment.domain.ReconciliationMismatchType;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentReconciliationExceptionResponse(
        Long id, ReconciliationEntityType entityType, Long entityId, String gatewayReferenceId,
        ReconciliationMismatchType mismatchType, String dbStatus, String gatewayStatus,
        BigDecimal dbAmount, BigDecimal gatewayAmount, String details, boolean resolved, Instant detectedAt) {

    public static PaymentReconciliationExceptionResponse from(PaymentReconciliationException e) {
        return new PaymentReconciliationExceptionResponse(e.getId(), e.getEntityType(), e.getEntityId(),
                e.getGatewayReferenceId(), e.getMismatchType(), e.getDbStatus(), e.getGatewayStatus(),
                e.getDbAmount(), e.getGatewayAmount(), e.getDetails(), e.isResolved(), e.getCreatedAt());
    }
}
