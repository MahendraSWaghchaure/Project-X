package com.projectx.payment.dto;

import java.math.BigDecimal;

public record CreatePaymentOrderResponse(
        Long paymentId, String razorpayOrderId, String razorpayKeyId, BigDecimal amount,
        BigDecimal discountAmount, int monthsCovered) {
}
