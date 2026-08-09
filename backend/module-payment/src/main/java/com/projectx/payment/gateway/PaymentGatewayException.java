package com.projectx.payment.gateway;

/** Wraps both "not configured" and real Razorpay API failures, so callers have one thing to catch. */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
