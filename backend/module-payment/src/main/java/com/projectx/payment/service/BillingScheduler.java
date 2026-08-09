package com.projectx.payment.service;

import java.time.LocalDate;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Three cron jobs implementing the product spec's billing cycle: invoices generated for the new
 * month, a daily late-fee sweep, and driver salaries disbursed on the 10th. All three are also
 * exposed for manual/admin-triggered runs via BillingAdminController, since waiting for the actual
 * calendar date isn't practical for testing or for recovering from a failed automatic run.
 */
@Component
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);

    private final FeeInvoiceService feeInvoiceService;
    private final PayrollDisbursementService payrollDisbursementService;

    public BillingScheduler(FeeInvoiceService feeInvoiceService, PayrollDisbursementService payrollDisbursementService) {
        this.feeInvoiceService = feeInvoiceService;
        this.payrollDisbursementService = payrollDisbursementService;
    }

    /** 1st of every month, 01:00 - generate this month's invoices (due the 7th). */
    @Scheduled(cron = "0 0 1 1 * *")
    public void generateMonthlyInvoices() {
        log.info("Running scheduled monthly fee invoice generation");
        feeInvoiceService.ensureInvoicesExistForMonth(YearMonth.now());
    }

    /** Every day, 02:00 - add the flat late fee to whatever's still unpaid past its due date. */
    @Scheduled(cron = "0 0 2 * * *")
    public void accrueLateFees() {
        log.info("Running scheduled late fee accrual");
        feeInvoiceService.accrueLateFeesForToday();
    }

    /** 10th of every month, 03:00 - disburse driver salaries. */
    @Scheduled(cron = "0 0 3 10 * *")
    public void disbursePayroll() {
        log.info("Running scheduled payroll disbursement");
        payrollDisbursementService.disburseForMonth(LocalDate.now().withDayOfMonth(1));
    }
}
