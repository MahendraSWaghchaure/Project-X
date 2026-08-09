package com.projectx.payment.dto;

import com.projectx.payment.domain.Payment;
import com.projectx.payment.domain.PaymentMode;
import com.projectx.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentResponse(
        Long id, Long parentId, Long schoolId, BigDecimal amount, BigDecimal discountAmount, int monthsCovered,
        PaymentMode mode, PaymentStatus status, String receiptNumber, List<Long> invoiceIds, Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getParentId(), payment.getSchoolId(), payment.getAmount(),
                payment.getDiscountAmount(), payment.getMonthsCovered(), payment.getMode(), payment.getStatus(),
                payment.getReceiptNumber(), payment.getCoveredInvoiceIds(), payment.getCreatedAt());
    }
}
