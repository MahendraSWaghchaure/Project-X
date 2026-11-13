package com.projectx.payment.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.payment.dto.PaymentReconciliationExceptionResponse;
import com.projectx.payment.repo.PaymentReconciliationExceptionRepository;
import com.projectx.payment.service.PaymentReconciliationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where the reconciliation job's findings are actually visible, not just sitting in a table - see
 * PaymentReconciliationService. PLATFORM_ADMIN-only, same as every other cross-tenant billing
 * endpoint in this module (see BillingAdminController).
 */
@RestController
@RequestMapping("/platform-admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class ReconciliationAdminController {

    private final PaymentReconciliationExceptionRepository exceptionRepository;
    private final PaymentReconciliationService reconciliationService;

    public ReconciliationAdminController(PaymentReconciliationExceptionRepository exceptionRepository,
                                          PaymentReconciliationService reconciliationService) {
        this.exceptionRepository = exceptionRepository;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/reconciliation-exceptions")
    public ApiResponse<PageResponse<PaymentReconciliationExceptionResponse>> listExceptions(
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(
                exceptionRepository.findAllByOrderByCreatedAtDesc(pageable).map(PaymentReconciliationExceptionResponse::from)));
    }

    @PostMapping("/reconciliation/run")
    public ApiResponse<PaymentReconciliationService.ReconciliationSummary> run() {
        return ApiResponse.ok(reconciliationService.reconcile());
    }
}
