package com.projectx.payment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tunables for PaymentReconciliationService - see application.yml for the defaults actually wired in. */
@ConfigurationProperties(prefix = "projectx.payment.reconciliation")
public class ReconciliationProperties {

    /** How far back to pull Payment/PayrollRecord rows for comparison on each run - keeps the job bounded instead of scanning the whole table forever. */
    private int lookbackDays = 7;

    /** Cap on how many platform admins get notified per run (same ceiling InquiryService uses for the same reason). */
    private int maxAdminsNotified = 20;

    public int getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public int getMaxAdminsNotified() {
        return maxAdminsNotified;
    }

    public void setMaxAdminsNotified(int maxAdminsNotified) {
        this.maxAdminsNotified = maxAdminsNotified;
    }
}
