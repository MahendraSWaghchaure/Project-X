package com.projectx.payment.web;

import com.projectx.common.web.ApiError;
import com.projectx.common.web.ApiResponse;
import com.projectx.payment.gateway.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Module-local advice for PaymentGatewayException - the shared GlobalExceptionHandler in
 * `common` can't reference it (common has no dependency on module-payment), so this covers it
 * here instead. Neither advice bean declares @Order by default, which makes Spring try them in
 * registration order rather than by exception-type specificity - common's catch-all
 * Exception.class handler was winning before this one ever got a look. @Order here makes sure
 * this bean is tried first.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExceptionHandler.class);

    /**
     * ex.getMessage() on a PaymentGatewayException can embed Razorpay's raw HTTP response body
     * (see RazorpayHttpClient) - logged in full here, but never returned to the API caller, which
     * would otherwise leak the payment gateway's internal error text/format to a parent-facing
     * client.
     */
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentGateway(PaymentGatewayException ex) {
        log.error("Payment gateway call failed", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ApiError.of("PAYMENT_GATEWAY_UNAVAILABLE",
                        "The payment gateway is currently unavailable. Please try again shortly.")));
    }
}
