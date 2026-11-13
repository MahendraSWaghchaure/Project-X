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

/**
 * One disagreement {@link PaymentReconciliationService} found between a {@code payments}/
 * {@code payroll_records} row and Razorpay's own fetch-order/fetch-payment/fetch-payout response
 * for the same id. Deliberately plain and append-only - this is a worklist for a human (surfaced
 * via {@code GET /platform-admin/reconciliation-exceptions}) and a notification trigger, not
 * something the reconciliation job itself tries to auto-correct; Razorpay is the source of truth
 * for what actually happened with real money, but fixing our row to match is a judgment call
 * (e.g. a payment Razorpay shows paid but we show CREATED might mean our verify() call simply
 * never ran - the fix is operational, not mechanical).
 *
 * <p>{@code resolved} lets the same still-open mismatch survive repeated daily runs as one row
 * instead of a new one every day - see {@code PaymentReconciliationService.alreadyFlagged}.
 */
@Entity
@Table(name = "payment_reconciliation_exceptions")
public class PaymentReconciliationException extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 24)
    private ReconciliationEntityType entityType;

    /** The Payment.id or PayrollRecord.id this exception is about. */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /** Razorpay order id (Payment) or payout id (PayrollRecord) - null only for the pathological case of a DB row with no gateway reference at all. */
    @Column(name = "gateway_reference_id")
    private String gatewayReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mismatch_type", nullable = false, length = 24)
    private ReconciliationMismatchType mismatchType;

    @Column(name = "db_status", length = 32)
    private String dbStatus;

    @Column(name = "gateway_status", length = 32)
    private String gatewayStatus;

    @Column(name = "db_amount", precision = 12, scale = 2)
    private BigDecimal dbAmount;

    @Column(name = "gateway_amount", precision = 12, scale = 2)
    private BigDecimal gatewayAmount;

    @Column(nullable = false, length = 1000)
    private String details;

    @Column(nullable = false)
    private boolean resolved = false;

    protected PaymentReconciliationException() {
    }

    public PaymentReconciliationException(ReconciliationEntityType entityType, Long entityId, String gatewayReferenceId,
                                           ReconciliationMismatchType mismatchType, String dbStatus, String gatewayStatus,
                                           BigDecimal dbAmount, BigDecimal gatewayAmount, String details) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.gatewayReferenceId = gatewayReferenceId;
        this.mismatchType = mismatchType;
        this.dbStatus = dbStatus;
        this.gatewayStatus = gatewayStatus;
        this.dbAmount = dbAmount;
        this.gatewayAmount = gatewayAmount;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public ReconciliationEntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getGatewayReferenceId() {
        return gatewayReferenceId;
    }

    public ReconciliationMismatchType getMismatchType() {
        return mismatchType;
    }

    public String getDbStatus() {
        return dbStatus;
    }

    public String getGatewayStatus() {
        return gatewayStatus;
    }

    public BigDecimal getDbAmount() {
        return dbAmount;
    }

    public BigDecimal getGatewayAmount() {
        return gatewayAmount;
    }

    public String getDetails() {
        return details;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void markResolved() {
        this.resolved = true;
    }
}
