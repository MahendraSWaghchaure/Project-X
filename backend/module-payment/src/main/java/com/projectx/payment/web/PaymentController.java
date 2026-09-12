package com.projectx.payment.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.payment.dto.AdvancePaymentRequest;
import com.projectx.payment.dto.CreatePaymentOrderResponse;
import com.projectx.payment.dto.PayInvoicesRequest;
import com.projectx.payment.dto.PaymentResponse;
import com.projectx.payment.dto.VerifyPaymentRequest;
import com.projectx.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/parents/me/payments/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<CreatePaymentOrderResponse> payExistingInvoices(@Valid @RequestBody PayInvoicesRequest request) {
        return ApiResponse.ok(paymentService.payExistingInvoices(request));
    }

    @PostMapping("/parents/me/payments/advance-orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<CreatePaymentOrderResponse> payInAdvance(@Valid @RequestBody AdvancePaymentRequest request) {
        return ApiResponse.ok(paymentService.payInAdvance(request));
    }

    @PostMapping("/parents/me/payments/{paymentId}/verify")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<PaymentResponse> verify(@PathVariable Long paymentId, @Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.ok(paymentService.verify(paymentId, request));
    }

    @GetMapping("/parents/me/payments")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<PageResponse<PaymentResponse>> listMine(@PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.ok(paymentService.listMine(pageable));
    }

    /** Doubles as the receipt view - a Payment IS the receipt once SUCCESS, see PaymentResponse.receiptNumber. */
    @GetMapping("/parents/me/payments/{paymentId}")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<PaymentResponse> getReceipt(@PathVariable Long paymentId) {
        return ApiResponse.ok(paymentService.getReceiptMine(paymentId));
    }

    @GetMapping("/schools/{schoolId}/payments")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<PageResponse<PaymentResponse>> listForSchool(@PathVariable Long schoolId,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.ok(paymentService.listForSchool(schoolId, pageable));
    }
}
