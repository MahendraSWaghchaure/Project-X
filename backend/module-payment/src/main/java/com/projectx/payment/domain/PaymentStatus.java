package com.projectx.payment.domain;

public enum PaymentStatus {
    /** Razorpay order created, waiting for the parent to complete checkout. */
    CREATED,
    SUCCESS,
    FAILED
}
