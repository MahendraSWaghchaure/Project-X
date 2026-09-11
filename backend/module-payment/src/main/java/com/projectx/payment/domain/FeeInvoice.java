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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row per child per billing month. dueDate is always the 7th of billingMonth; anything still
 * unpaid after that accrues lateFeeAmount at a flat rate per day (see LateFeeAccrualJob) until
 * paid, cancelled, or the late fee is waived. lastLateFeeAccrualDate stops the daily job from
 * double-charging the same day if it ever runs more than once.
 *
 * <p>lateFeeWaived is deliberately a separate flag rather than a WAIVED status value - waiving
 * only forgives the late-fee penalty, the base fee is still owed and the invoice still needs to
 * go through the normal payment flow, so it stays PENDING/OVERDUE rather than looking settled.
 */
@Entity
@Table(name = "fee_invoices")
public class FeeInvoice extends Auditable {

    public static final BigDecimal LATE_FEE_PER_DAY = BigDecimal.valueOf(50);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Guards markPaid()/accrueLateFeeForToday() racing on the same invoice from silently losing one write. */
    @Version
    private Long version;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "late_fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeeInvoiceStatus status = FeeInvoiceStatus.PENDING;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "last_late_fee_accrual_date")
    private LocalDate lastLateFeeAccrualDate;

    @Column(name = "late_fee_waived", nullable = false)
    private boolean lateFeeWaived = false;

    protected FeeInvoice() {
    }

    public FeeInvoice(Long childId, Long parentId, Long schoolId, LocalDate billingMonth, BigDecimal baseAmount,
                       LocalDate dueDate) {
        this.childId = childId;
        this.parentId = parentId;
        this.schoolId = schoolId;
        this.billingMonth = billingMonth;
        this.baseAmount = baseAmount;
        this.dueDate = dueDate;
    }

    public Long getId() {
        return id;
    }

    public Long getChildId() {
        return childId;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getLateFeeAmount() {
        return lateFeeAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public FeeInvoiceStatus getStatus() {
        return status;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public LocalDate getLastLateFeeAccrualDate() {
        return lastLateFeeAccrualDate;
    }

    public boolean isLateFeeWaived() {
        return lateFeeWaived;
    }

    /** baseAmount - discountAmount + lateFeeAmount, floored at zero so a fully-discounted invoice never shows negative. */
    public BigDecimal getTotalAmount() {
        BigDecimal total = baseAmount.subtract(discountAmount).add(lateFeeAmount);
        return total.max(BigDecimal.ZERO);
    }

    public boolean isSettled() {
        return status == FeeInvoiceStatus.PAID || status == FeeInvoiceStatus.CANCELLED;
    }

    /** Called by LateFeeAccrualJob once per calendar day for every unpaid invoice past its due date. */
    public void accrueLateFeeForToday(LocalDate today) {
        if (isSettled() || lateFeeWaived || !today.isAfter(dueDate)) {
            return;
        }
        if (today.equals(lastLateFeeAccrualDate)) {
            return;
        }
        this.lateFeeAmount = lateFeeAmount.add(LATE_FEE_PER_DAY);
        this.lastLateFeeAccrualDate = today;
        this.status = FeeInvoiceStatus.OVERDUE;
    }

    public void markPaid(Long paymentId) {
        this.paymentId = paymentId;
        this.status = FeeInvoiceStatus.PAID;
    }

    /** Late-fee waiver approval: forgives whatever late fee has accrued so far and stops further accrual; base amount is still owed. */
    public void waiveLateFee() {
        this.lateFeeAmount = BigDecimal.ZERO;
        this.lateFeeWaived = true;
        if (status == FeeInvoiceStatus.OVERDUE) {
            this.status = FeeInvoiceStatus.PENDING;
        }
    }
}
