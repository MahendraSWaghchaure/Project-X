package com.projectx.payment.domain;

/** Which of this module's two Razorpay-backed tables a reconciliation exception row is about. */
public enum ReconciliationEntityType {
    PAYMENT,
    PAYROLL_RECORD
}
