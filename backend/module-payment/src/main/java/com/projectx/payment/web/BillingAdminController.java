package com.projectx.payment.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.payment.service.FeeInvoiceService;
import com.projectx.payment.service.PayrollDisbursementService;
import java.time.YearMonth;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Manual triggers for BillingScheduler's three jobs - waiting for the actual 1st/7th/10th isn't practical for testing or recovering a failed automatic run. */
@RestController
@RequestMapping("/billing")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class BillingAdminController {

    private final FeeInvoiceService feeInvoiceService;
    private final PayrollDisbursementService payrollDisbursementService;

    public BillingAdminController(FeeInvoiceService feeInvoiceService, PayrollDisbursementService payrollDisbursementService) {
        this.feeInvoiceService = feeInvoiceService;
        this.payrollDisbursementService = payrollDisbursementService;
    }

    @PostMapping("/generate-invoices")
    public ApiResponse<Void> generateInvoices() {
        feeInvoiceService.ensureInvoicesExistForMonth(YearMonth.now());
        return ApiResponse.ok(null);
    }

    @PostMapping("/accrue-late-fees")
    public ApiResponse<Void> accrueLateFees() {
        feeInvoiceService.accrueLateFeesForToday();
        return ApiResponse.ok(null);
    }

    @PostMapping("/run-payroll")
    public ApiResponse<Void> runPayroll() {
        payrollDisbursementService.disburseForCurrentMonth();
        return ApiResponse.ok(null);
    }
}
