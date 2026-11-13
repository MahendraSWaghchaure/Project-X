package com.projectx.payment.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * driverId is a cross-module id reference (module-people), same discipline as elsewhere.
 * incentiveAmount is the "covered another driver's route during a breakdown" bonus from the
 * product spec - filling it in from actual cover-duty records is a later pass; today it can be
 * set manually by a school admin when running payroll.
 *
 * <p>transactionReference/disbursedAt are populated by PayrollDisbursementService once the
 * RazorpayX payout call succeeds (or failureReason if it didn't) - see that class for what
 * "real disbursement" actually means here.
 */
@Entity
@Table(name = "payroll_records")
public class PayrollRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    /** Always the 1st of the payroll month. */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "overtime_hours", precision = 6, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "incentive_amount", precision = 12, scale = 2)
    private BigDecimal incentiveAmount = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 12, scale = 2)
    private BigDecimal totalPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayrollStatus status = PayrollStatus.PENDING;

    @Column(name = "transaction_reference")
    private String transactionReference;

    /**
     * The Razorpay payout id itself (e.g. "pout_..."), distinct from transactionReference (which
     * prefers the bank UTR once assigned - see RazorpayPayoutClient.createPayout). Needed because
     * reconciliation has to fetch-by-id against Razorpay's payouts API, and a UTR isn't a valid
     * payout id. Null for any record paid before this column existed, or never paid at all.
     */
    @Column(name = "razorpay_payout_id")
    private String razorpayPayoutId;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    protected PayrollRecord() {
    }

    public PayrollRecord(Long driverId, LocalDate periodStart, BigDecimal baseSalary) {
        this.driverId = driverId;
        this.periodStart = periodStart;
        this.baseSalary = baseSalary;
    }

    public Long getId() {
        return id;
    }

    public Long getDriverId() {
        return driverId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public BigDecimal getIncentiveAmount() {
        return incentiveAmount;
    }

    public void setIncentiveAmount(BigDecimal incentiveAmount) {
        this.incentiveAmount = incentiveAmount;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public PayrollStatus getStatus() {
        return status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getRazorpayPayoutId() {
        return razorpayPayoutId;
    }

    /** Set before markPaid() so the id survives even if the caller only has the UTR-preferring transactionReference. */
    public void setRazorpayPayoutId(String razorpayPayoutId) {
        this.razorpayPayoutId = razorpayPayoutId;
    }

    public Instant getDisbursedAt() {
        return disbursedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void markProcessing() {
        this.status = PayrollStatus.PROCESSING;
    }

    public void markPaid(BigDecimal totalPaid, String transactionReference) {
        this.totalPaid = totalPaid;
        this.status = PayrollStatus.PAID;
        this.transactionReference = transactionReference;
        this.disbursedAt = Instant.now();
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = PayrollStatus.FAILED;
        this.failureReason = reason;
    }
}
