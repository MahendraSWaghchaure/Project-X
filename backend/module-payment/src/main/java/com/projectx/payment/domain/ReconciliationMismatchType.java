package com.projectx.payment.domain;

/** What kind of disagreement was found between our DB row and Razorpay's own record of the same order/payout. */
public enum ReconciliationMismatchType {
    /** e.g. we think a Payment is still CREATED (pending) but Razorpay's order shows it as paid, or vice versa. */
    STATUS_MISMATCH,
    /** Same order/payout, same apparent status, but the amount Razorpay has on file differs from ours. */
    AMOUNT_MISMATCH,
    /** Our row references a Razorpay order/payout id that Razorpay itself has no record of at all. */
    MISSING_AT_GATEWAY
}
