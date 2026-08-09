package com.projectx.payment.dto;

import com.projectx.payment.domain.FeeInvoice;
import com.projectx.payment.domain.FeeInvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeInvoiceResponse(
        Long id, Long childId, Long parentId, Long schoolId, LocalDate billingMonth, BigDecimal baseAmount,
        BigDecimal discountAmount, BigDecimal lateFeeAmount, BigDecimal totalAmount, LocalDate dueDate,
        FeeInvoiceStatus status, boolean lateFeeWaived, Long paymentId) {

    public static FeeInvoiceResponse from(FeeInvoice invoice) {
        return new FeeInvoiceResponse(invoice.getId(), invoice.getChildId(), invoice.getParentId(), invoice.getSchoolId(),
                invoice.getBillingMonth(), invoice.getBaseAmount(), invoice.getDiscountAmount(), invoice.getLateFeeAmount(),
                invoice.getTotalAmount(), invoice.getDueDate(), invoice.getStatus(), invoice.isLateFeeWaived(),
                invoice.getPaymentId());
    }
}
