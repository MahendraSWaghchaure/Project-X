package com.projectx.payment.web;

import com.projectx.common.web.ApiError;
import com.projectx.common.web.ApiResponse;
import com.projectx.payment.gateway.PaymentGatewayException;
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

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentGateway(PaymentGatewayException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ApiError.of("PAYMENT_GATEWAY_UNAVAILABLE", ex.getMessage())));
    }
}
