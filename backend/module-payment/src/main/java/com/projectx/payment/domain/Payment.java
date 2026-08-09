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
import java.util.List;
import java.util.stream.Collectors;

/**
 * One Razorpay checkout attempt. Created in CREATED status when the parent starts checkout
 * (holds a Razorpay order id), flips to SUCCESS once the signature on the completed payment is
 * verified (see PaymentService.verify) or FAILED if verification fails / the parent abandons it.
 * A single successful payment can settle more than one fee_invoice at once (advance payment).
 *
 * <p>coveredInvoiceIds is a comma-separated fee_invoices.id list rather than a join table - a
 * payment covers at most ~12 invoices at this scale, so a delimited column is simpler than a
 * whole extra table for the same information.
 */
@Entity
@Table(name = "payments")
public class Payment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "months_covered", nullable = false)
    private int monthsCovered;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PaymentMode mode;

    @Column(name = "razorpay_order_id", nullable = false, unique = true)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(name = "receipt_number", unique = true)
    private String receiptNumber;

    @Column(name = "covered_invoice_ids", nullable = false)
    private String coveredInvoiceIds;

    protected Payment() {
    }

    public Payment(Long parentId, Long schoolId, BigDecimal amount, BigDecimal discountAmount, int monthsCovered,
                    String razorpayOrderId, List<Long> coveredInvoiceIds) {
        this.parentId = parentId;
        this.schoolId = schoolId;
        this.amount = amount;
        this.discountAmount = discountAmount;
        this.monthsCovered = monthsCovered;
        this.razorpayOrderId = razorpayOrderId;
        this.coveredInvoiceIds = coveredInvoiceIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public int getMonthsCovered() {
        return monthsCovered;
    }

    public PaymentMode getMode() {
        return mode;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public List<Long> getCoveredInvoiceIds() {
        return List.of(coveredInvoiceIds.split(",")).stream().map(Long::valueOf).collect(Collectors.toList());
    }

    public void markSuccess(String razorpayPaymentId, String razorpaySignature, PaymentMode mode, String receiptNumber) {
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpaySignature = razorpaySignature;
        this.mode = mode;
        this.status = PaymentStatus.SUCCESS;
        this.receiptNumber = receiptNumber;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }
}
