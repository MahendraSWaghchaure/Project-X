package com.projectx.payment.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.web.PageResponse;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.payment.domain.FeeInvoice;
import com.projectx.payment.domain.Payment;
import com.projectx.payment.domain.PaymentMode;
import com.projectx.payment.domain.PaymentStatus;
import com.projectx.payment.dto.AdvancePaymentRequest;
import com.projectx.payment.dto.CreatePaymentOrderResponse;
import com.projectx.payment.dto.PayInvoicesRequest;
import com.projectx.payment.dto.PaymentResponse;
import com.projectx.payment.dto.VerifyPaymentRequest;
import com.projectx.payment.gateway.RazorpayOrderService;
import com.projectx.payment.repo.FeeInvoiceRepository;
import com.projectx.payment.repo.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final FeeInvoiceService feeInvoiceService;
    private final ParentProfileRepository parentProfileRepository;
    private final ChildRepository childRepository;
    private final RazorpayOrderService razorpayOrderService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, FeeInvoiceRepository feeInvoiceRepository,
                           FeeInvoiceService feeInvoiceService, ParentProfileRepository parentProfileRepository,
                           ChildRepository childRepository, RazorpayOrderService razorpayOrderService,
                           ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.feeInvoiceRepository = feeInvoiceRepository;
        this.feeInvoiceService = feeInvoiceService;
        this.parentProfileRepository = parentProfileRepository;
        this.childRepository = childRepository;
        this.razorpayOrderService = razorpayOrderService;
        this.eventPublisher = eventPublisher;
    }

    /** Normal (or catch-up-on-overdue) payment: parent pays specific invoices they already owe, at face value - no advance-pay discount. */
    public CreatePaymentOrderResponse payExistingInvoices(PayInvoicesRequest request) {
        ParentProfile parent = currentParent();
        List<FeeInvoice> invoices = feeInvoiceRepository.findAllById(request.feeInvoiceIds());
        if (invoices.size() != request.feeInvoiceIds().size()) {
            throw new ResourceNotFoundException("One or more invoices not found");
        }
        for (FeeInvoice invoice : invoices) {
            if (!invoice.getParentId().equals(parent.getId())) {
                throw new ForbiddenOperationException("Not your invoice");
            }
            if (invoice.isSettled()) {
                throw new ConflictException("Invoice " + invoice.getId() + " is already paid");
            }
        }
        BigDecimal total = invoices.stream().map(FeeInvoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return createOrderFor(parent, invoices.get(0).getSchoolId(), invoices, total, BigDecimal.ZERO, invoices.size());
    }

    /** Advance payment: generates (if needed) the next N unbilled months for this child and pays them all at once, at a discount. */
    public CreatePaymentOrderResponse payInAdvance(AdvancePaymentRequest request) {
        ParentProfile parent = currentParent();
        Child child = childRepository.findById(request.childId())
                .orElseThrow(() -> ResourceNotFoundException.of("Child", request.childId()));
        boolean isParentsChild = parent.getId().equals(child.getPrimaryParentId()) || parent.getId().equals(child.getSecondaryParentId());
        if (!isParentsChild) {
            throw new ForbiddenOperationException("Not your child");
        }

        List<FeeInvoice> invoices = feeInvoiceService.ensureAdvanceInvoices(child, request.months());
        BigDecimal subtotal = invoices.stream().map(FeeInvoice::getBaseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = AdvancePaymentDiscount.discountFor(subtotal, request.months());
        distributeDiscountAcross(invoices, discount);
        BigDecimal total = subtotal.subtract(discount);

        return createOrderFor(parent, child.getSchoolId(), invoices, total, discount, request.months());
    }

    private void distributeDiscountAcross(List<FeeInvoice> invoices, BigDecimal totalDiscount) {
        if (totalDiscount.signum() == 0 || invoices.isEmpty()) {
            return;
        }
        BigDecimal perInvoice = totalDiscount.divide(BigDecimal.valueOf(invoices.size()), 2, RoundingMode.HALF_UP);
        invoices.forEach(invoice -> invoice.setDiscountAmount(perInvoice));
    }

    private CreatePaymentOrderResponse createOrderFor(ParentProfile parent, Long schoolId, List<FeeInvoice> invoices,
                                                        BigDecimal totalAmount, BigDecimal discountAmount, int monthsCovered) {
        String receiptRef = "parent-%d-%d".formatted(parent.getId(), System.currentTimeMillis());
        String razorpayOrderId = razorpayOrderService.createOrder(totalAmount, receiptRef);

        Payment payment = new Payment(parent.getId(), schoolId, totalAmount, discountAmount, monthsCovered, razorpayOrderId,
                invoices.stream().map(FeeInvoice::getId).toList());
        payment = paymentRepository.save(payment);

        return new CreatePaymentOrderResponse(payment.getId(), razorpayOrderId, razorpayOrderService.getKeyId(),
                totalAmount, discountAmount, monthsCovered);
    }

    /** Called by the frontend once Razorpay Checkout completes - verifies the signature server-side before trusting anything the client said. */
    public PaymentResponse verify(Long paymentId, VerifyPaymentRequest request) {
        ParentProfile parent = currentParent();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", paymentId));
        if (!payment.getParentId().equals(parent.getId())) {
            throw new ForbiddenOperationException("Not your payment");
        }
        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ConflictException("This payment was already " + payment.getStatus());
        }
        if (!payment.getRazorpayOrderId().equals(request.razorpayOrderId())) {
            throw new ForbiddenOperationException("Order id does not match this payment");
        }

        boolean signatureValid = razorpayOrderService.verifySignature(
                request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
        if (!signatureValid) {
            payment.markFailed();
            eventPublisher.publishEvent(AuditEvent.of("PAYMENT_VERIFICATION_FAILED", "PAYMENT", payment.getId(),
                    payment.getSchoolId(), "Signature verification failed for Razorpay order " + request.razorpayOrderId()));
            throw new ForbiddenOperationException("Payment signature verification failed");
        }

        PaymentMode mode = PaymentMode.fromRazorpayMethod(safeFetchMethod(request.razorpayPaymentId()));
        String receiptNumber = "RCPT-%d-%d".formatted(Year.now().getValue(), System.currentTimeMillis());
        payment.markSuccess(request.razorpayPaymentId(), request.razorpaySignature(), mode, receiptNumber);

        for (Long invoiceId : payment.getCoveredInvoiceIds()) {
            feeInvoiceRepository.findById(invoiceId).ifPresent(invoice -> invoice.markPaid(payment.getId()));
        }
        eventPublisher.publishEvent(AuditEvent.of("PAYMENT_SUCCEEDED", "PAYMENT", payment.getId(), payment.getSchoolId(),
                "Payment of " + payment.getAmount() + " verified and marked successful, receipt " + receiptNumber));
        return PaymentResponse.from(payment);
    }

    /** Best-effort: the receipt still works (mode just shows as OTHER) if this lookup call itself fails for any reason. */
    private String safeFetchMethod(String razorpayPaymentId) {
        try {
            return razorpayOrderService.fetchPaymentMethod(razorpayPaymentId);
        } catch (RuntimeException e) {
            log.warn("Could not fetch Razorpay payment method for {}: {}", razorpayPaymentId, e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listMine(Pageable pageable) {
        ParentProfile parent = currentParent();
        return PageResponse.from(
                paymentRepository.findByParentIdOrderByCreatedAtDesc(parent.getId(), pageable).map(PaymentResponse::from));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getReceiptMine(Long paymentId) {
        ParentProfile parent = currentParent();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", paymentId));
        if (!payment.getParentId().equals(parent.getId())) {
            throw new ForbiddenOperationException("Not your payment");
        }
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForSchool(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return paymentRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream().map(PaymentResponse::from).toList();
    }

    private ParentProfile currentParent() {
        CurrentUser user = CurrentUserContext.get();
        return parentProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for the current user"));
    }
}
