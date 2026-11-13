package com.projectx.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.notification.service.NotificationDispatchService;
import com.projectx.payment.domain.Payment;
import com.projectx.payment.domain.PaymentReconciliationException;
import com.projectx.payment.domain.PaymentStatus;
import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.domain.ReconciliationEntityType;
import com.projectx.payment.domain.ReconciliationMismatchType;
import com.projectx.payment.gateway.PaymentGatewayException;
import com.projectx.payment.gateway.RazorpayOrderService;
import com.projectx.payment.gateway.RazorpayOrderService.RazorpayOrderSnapshot;
import com.projectx.payment.gateway.RazorpayPayoutClient;
import com.projectx.payment.repo.PaymentReconciliationExceptionRepository;
import com.projectx.payment.repo.PaymentRepository;
import com.projectx.payment.repo.PayrollRecordRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Pure Mockito unit test - no Spring context, no real Razorpay calls. This is the same pattern
 * this codebase's README documents for module-payment's test tier ("Unit tests only (fast, no
 * external services)" via {@code mvn -pl module-payment test}), and the only existing payment
 * test (AdvancePaymentDiscountTest) is already a plain unit test with no network/DB dependency -
 * there is no existing Razorpay-mocking test in this module to follow, so RazorpayOrderService
 * and RazorpayPayoutClient (both plain, non-final, constructor-injected classes) are mocked
 * directly here rather than exercising RazorpayHttpClient or any real HTTP call.
 */
class PaymentReconciliationServiceTest {

    private PaymentRepository paymentRepository;
    private PayrollRecordRepository payrollRecordRepository;
    private PaymentReconciliationExceptionRepository exceptionRepository;
    private RazorpayOrderService razorpayOrderService;
    private RazorpayPayoutClient razorpayPayoutClient;
    private UserAccountRepository userAccountRepository;
    private NotificationDispatchService dispatchService;
    private PaymentReconciliationService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        payrollRecordRepository = mock(PayrollRecordRepository.class);
        exceptionRepository = mock(PaymentReconciliationExceptionRepository.class);
        razorpayOrderService = mock(RazorpayOrderService.class);
        razorpayPayoutClient = mock(RazorpayPayoutClient.class);
        userAccountRepository = mock(UserAccountRepository.class);
        dispatchService = mock(NotificationDispatchService.class);

        ReconciliationProperties properties = new ReconciliationProperties();
        service = new PaymentReconciliationService(paymentRepository, payrollRecordRepository, exceptionRepository,
                razorpayOrderService, razorpayPayoutClient, userAccountRepository, dispatchService, properties);

        // No payroll records in play for the payment-focused tests below - keeps each test about one thing.
        when(razorpayPayoutClient.isEnabled()).thenReturn(false);
        // save() returning its argument is standard for exercising the "was a row inserted" path without a real DB.
        when(exceptionRepository.save(any(PaymentReconciliationException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UserAccount activeAdmin(long id) {
        UserAccount admin = mock(UserAccount.class);
        when(admin.getId()).thenReturn(id);
        when(admin.isActive()).thenReturn(true);
        return admin;
    }

    @Test
    void matchingPaymentIsNotFlaggedOrNotified() {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(1L);
        when(payment.getRazorpayOrderId()).thenReturn("order_match");
        when(payment.getStatus()).thenReturn(PaymentStatus.SUCCESS);
        when(payment.getAmount()).thenReturn(new BigDecimal("1000.00"));

        when(razorpayOrderService.isEnabled()).thenReturn(true);
        when(paymentRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any())).thenReturn(List.of(payment));
        when(razorpayOrderService.fetchOrder("order_match"))
                .thenReturn(new RazorpayOrderSnapshot("paid", new BigDecimal("1000.00"), new BigDecimal("1000.00")));

        PaymentReconciliationService.ReconciliationSummary summary = service.reconcile();

        assertThat(summary.paymentsChecked()).isEqualTo(1);
        assertThat(summary.newExceptions()).isZero();
        verify(exceptionRepository, never()).save(any());
        verify(dispatchService, never()).dispatch(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void mismatchedPaymentIsFlaggedAndAdminNotified() {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(2L);
        when(payment.getRazorpayOrderId()).thenReturn("order_mismatch");
        // DB thinks this payment succeeded...
        when(payment.getStatus()).thenReturn(PaymentStatus.SUCCESS);
        when(payment.getAmount()).thenReturn(new BigDecimal("500.00"));

        when(razorpayOrderService.isEnabled()).thenReturn(true);
        when(paymentRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any())).thenReturn(List.of(payment));
        // ...but Razorpay says the order was never actually paid.
        when(razorpayOrderService.fetchOrder("order_mismatch"))
                .thenReturn(new RazorpayOrderSnapshot("created", new BigDecimal("500.00"), BigDecimal.ZERO));
        when(exceptionRepository.existsByEntityTypeAndEntityIdAndMismatchTypeAndResolvedFalse(
                ReconciliationEntityType.PAYMENT, 2L, ReconciliationMismatchType.STATUS_MISMATCH)).thenReturn(false);

        UserAccount admin = activeAdmin(99L);
        when(userAccountRepository.findByRole(eq(Role.PLATFORM_ADMIN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(admin)));

        PaymentReconciliationService.ReconciliationSummary summary = service.reconcile();

        assertThat(summary.newExceptions()).isEqualTo(1);
        ArgumentCaptor<PaymentReconciliationException> captor = ArgumentCaptor.forClass(PaymentReconciliationException.class);
        verify(exceptionRepository).save(captor.capture());
        PaymentReconciliationException saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(ReconciliationEntityType.PAYMENT);
        assertThat(saved.getEntityId()).isEqualTo(2L);
        assertThat(saved.getMismatchType()).isEqualTo(ReconciliationMismatchType.STATUS_MISMATCH);
        assertThat(saved.getDbStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getGatewayStatus()).isEqualTo("created");

        verify(dispatchService).dispatch(eq(99L), eq("payment.RECONCILIATION_MISMATCH"), anyString(), anyString());
    }

    @Test
    void alreadyOpenExceptionIsNotFlaggedAgainOnRerun() {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(3L);
        when(payment.getRazorpayOrderId()).thenReturn("order_repeat");
        when(payment.getStatus()).thenReturn(PaymentStatus.SUCCESS);
        when(payment.getAmount()).thenReturn(new BigDecimal("250.00"));

        when(razorpayOrderService.isEnabled()).thenReturn(true);
        when(paymentRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any())).thenReturn(List.of(payment));
        when(razorpayOrderService.fetchOrder("order_repeat"))
                .thenReturn(new RazorpayOrderSnapshot("created", new BigDecimal("250.00"), BigDecimal.ZERO));
        // Simulates yesterday's run already having flagged this exact mismatch and it's still unresolved.
        when(exceptionRepository.existsByEntityTypeAndEntityIdAndMismatchTypeAndResolvedFalse(
                ReconciliationEntityType.PAYMENT, 3L, ReconciliationMismatchType.STATUS_MISMATCH)).thenReturn(true);

        PaymentReconciliationService.ReconciliationSummary summary = service.reconcile();

        assertThat(summary.newExceptions()).isZero();
        verify(exceptionRepository, never()).save(any());
        verify(dispatchService, never()).dispatch(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void missingAtGatewayIsFlaggedWhenRazorpayHasNoRecordOfTheOrder() {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(4L);
        when(payment.getRazorpayOrderId()).thenReturn("order_ghost");
        when(payment.getStatus()).thenReturn(PaymentStatus.SUCCESS);
        when(payment.getAmount()).thenReturn(new BigDecimal("750.00"));

        when(razorpayOrderService.isEnabled()).thenReturn(true);
        when(paymentRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any())).thenReturn(List.of(payment));
        when(razorpayOrderService.fetchOrder("order_ghost")).thenThrow(new PaymentGatewayException("404 not found"));
        when(exceptionRepository.existsByEntityTypeAndEntityIdAndMismatchTypeAndResolvedFalse(
                ReconciliationEntityType.PAYMENT, 4L, ReconciliationMismatchType.MISSING_AT_GATEWAY)).thenReturn(false);
        when(userAccountRepository.findByRole(eq(Role.PLATFORM_ADMIN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PaymentReconciliationService.ReconciliationSummary summary = service.reconcile();

        assertThat(summary.newExceptions()).isEqualTo(1);
        ArgumentCaptor<PaymentReconciliationException> captor = ArgumentCaptor.forClass(PaymentReconciliationException.class);
        verify(exceptionRepository).save(captor.capture());
        assertThat(captor.getValue().getMismatchType()).isEqualTo(ReconciliationMismatchType.MISSING_AT_GATEWAY);
    }

    @Test
    void payoutsSkippedEntirelyWhenRazorpayXNotConfigured() {
        when(razorpayOrderService.isEnabled()).thenReturn(false);
        when(razorpayPayoutClient.isEnabled()).thenReturn(false);

        PaymentReconciliationService.ReconciliationSummary summary = service.reconcile();

        assertThat(summary.paymentsChecked()).isZero();
        assertThat(summary.payrollRecordsChecked()).isZero();
        assertThat(summary.newExceptions()).isZero();
        verify(paymentRepository, never()).findByCreatedAtAfterOrderByCreatedAtDesc(any());
        verify(payrollRecordRepository, never()).findByRazorpayPayoutIdIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(any());
    }

    @Test
    void matchingPayrollPayoutIsNotFlagged() {
        when(razorpayOrderService.isEnabled()).thenReturn(false);
        when(razorpayPayoutClient.isEnabled()).thenReturn(true);

        PayrollRecord record = mock(PayrollRecord.class);
        when(record.getId()).thenReturn(10L);
        when(record.getRazorpayPayoutId()).thenReturn("pout_match");
        when(record.getStatus()).thenReturn(com.projectx.payment.domain.PayrollStatus.PAID);
        when(record.getTotalPaid()).thenReturn(new BigDecimal("20000.00"));

        when(payrollRecordRepository.findByRazorpayPayoutIdIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(record));
        when(razorpayPayoutClient.fetchPayout("pout_match"))
                .thenReturn(new RazorpayPayoutClient.PayoutSnapshot("processed", new BigDecimal("20000.00")));

        PaymentReconciliationService.ReconciliationSummary summary = service.reconcile();

        assertThat(summary.payrollRecordsChecked()).isEqualTo(1);
        assertThat(summary.newExceptions()).isZero();
        verify(exceptionRepository, never()).save(any());
    }

    @Test
    void reversedPayoutStillMarkedPaidInDbIsFlagged() {
        when(razorpayOrderService.isEnabled()).thenReturn(false);
        when(razorpayPayoutClient.isEnabled()).thenReturn(true);

        PayrollRecord record = mock(PayrollRecord.class);
        when(record.getId()).thenReturn(11L);
        when(record.getRazorpayPayoutId()).thenReturn("pout_reversed");
        when(record.getStatus()).thenReturn(com.projectx.payment.domain.PayrollStatus.PAID);
        when(record.getTotalPaid()).thenReturn(new BigDecimal("18000.00"));

        when(payrollRecordRepository.findByRazorpayPayoutIdIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(record));
        when(razorpayPayoutClient.fetchPayout("pout_reversed"))
                .thenReturn(new RazorpayPayoutClient.PayoutSnapshot("reversed", new BigDecimal("18000.00")));
        when(exceptionRepository.existsByEntityTypeAndEntityIdAndMismatchTypeAndResolvedFalse(
                ReconciliationEntityType.PAYROLL_RECORD, 11L, ReconciliationMismatchType.STATUS_MISMATCH)).thenReturn(false);
        when(userAccountRepository.findByRole(eq(Role.PLATFORM_ADMIN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PaymentReconciliationService.ReconciliationSummary summary = service.reconcile();

        assertThat(summary.newExceptions()).isEqualTo(1);
        ArgumentCaptor<PaymentReconciliationException> captor = ArgumentCaptor.forClass(PaymentReconciliationException.class);
        verify(exceptionRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo(ReconciliationEntityType.PAYROLL_RECORD);
        assertThat(captor.getValue().getMismatchType()).isEqualTo(ReconciliationMismatchType.STATUS_MISMATCH);
    }
}
