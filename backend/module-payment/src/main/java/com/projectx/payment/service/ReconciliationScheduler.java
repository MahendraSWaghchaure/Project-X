package com.projectx.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily Razorpay-vs-DB reconciliation (see PaymentReconciliationService). Also exposed for a
 * manual/admin-triggered run via ReconciliationAdminController, same "don't wait for the clock"
 * reasoning as BillingScheduler's three jobs.
 */
@Component
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    private final PaymentReconciliationService reconciliationService;

    public ReconciliationScheduler(PaymentReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /** Every day, 04:00 - after the billing cycle jobs (01:00/02:00/03:00, see BillingScheduler) have had a chance to run. */
    @Scheduled(cron = "0 0 4 * * *")
    public void reconcile() {
        log.info("Running scheduled payment/payout reconciliation against Razorpay");
        reconciliationService.reconcile();
    }
}
