package com.projectx.payment.service;

import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.notification.service.NotificationDispatchService;
import com.projectx.payment.domain.Payment;
import com.projectx.payment.domain.PaymentReconciliationException;
import com.projectx.payment.domain.PaymentStatus;
import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.domain.PayrollStatus;
import com.projectx.payment.domain.ReconciliationEntityType;
import com.projectx.payment.domain.ReconciliationMismatchType;
import com.projectx.payment.gateway.PaymentGatewayException;
import com.projectx.payment.gateway.RazorpayOrderService;
import com.projectx.payment.gateway.RazorpayPayoutClient;
import com.projectx.payment.repo.PaymentReconciliationExceptionRepository;
import com.projectx.payment.repo.PaymentRepository;
import com.projectx.payment.repo.PayrollRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compares this module's own record of recent Payments/PayrollRecords against Razorpay's own
 * fetch-order / fetch-payout responses for the same ids, and flags any disagreement as a new
 * {@link PaymentReconciliationException} row - the whole point being that a Razorpay webhook
 * miss, a crashed verify() call, or a payout that got reversed after the fact leaves our own
 * tables as the only thing that silently drifted from what actually happened with real money.
 *
 * <p>Run by {@link ReconciliationScheduler} daily, or on demand via
 * {@code POST /platform-admin/reconciliation/run} (see the admin controller). Entirely inert
 * (checks nothing, flags nothing, notifies nobody) when Razorpay/RazorpayX aren't configured -
 * exactly the same off-by-default discipline every other Razorpay-touching code path in this
 * module follows, so this is safe to leave scheduled in any environment, including one with no
 * real credentials at all.
 */
@Service
@Transactional
@EnableConfigurationProperties(ReconciliationProperties.class)
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);
    private static final Set<String> TERMINAL_FAILED_PAYOUT_STATUSES = Set.of("failed", "reversed", "rejected", "cancelled");

    private final PaymentRepository paymentRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final PaymentReconciliationExceptionRepository exceptionRepository;
    private final RazorpayOrderService razorpayOrderService;
    private final RazorpayPayoutClient razorpayPayoutClient;
    private final UserAccountRepository userAccountRepository;
    private final NotificationDispatchService dispatchService;
    private final ReconciliationProperties properties;

    public PaymentReconciliationService(PaymentRepository paymentRepository, PayrollRecordRepository payrollRecordRepository,
                                         PaymentReconciliationExceptionRepository exceptionRepository,
                                         RazorpayOrderService razorpayOrderService, RazorpayPayoutClient razorpayPayoutClient,
                                         UserAccountRepository userAccountRepository, NotificationDispatchService dispatchService,
                                         ReconciliationProperties properties) {
        this.paymentRepository = paymentRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.exceptionRepository = exceptionRepository;
        this.razorpayOrderService = razorpayOrderService;
        this.razorpayPayoutClient = razorpayPayoutClient;
        this.userAccountRepository = userAccountRepository;
        this.dispatchService = dispatchService;
        this.properties = properties;
    }

    public record ReconciliationSummary(int paymentsChecked, int payrollRecordsChecked, int newExceptions) {
    }

    public ReconciliationSummary reconcile() {
        Instant since = Instant.now().minus(properties.getLookbackDays(), ChronoUnit.DAYS);
        List<PaymentReconciliationException> newExceptions = new ArrayList<>();

        int paymentsChecked = reconcilePayments(since, newExceptions);
        int payrollChecked = reconcilePayroll(since, newExceptions);

        if (!newExceptions.isEmpty()) {
            notifyPlatformAdmins(newExceptions);
        }
        log.info("Reconciliation run: {} payments checked, {} payroll records checked, {} new exceptions",
                paymentsChecked, payrollChecked, newExceptions.size());
        return new ReconciliationSummary(paymentsChecked, payrollChecked, newExceptions.size());
    }

    private int reconcilePayments(Instant since, List<PaymentReconciliationException> newExceptions) {
        if (!razorpayOrderService.isEnabled()) {
            log.info("Razorpay Orders not configured - skipping payment reconciliation");
            return 0;
        }
        List<Payment> payments = paymentRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);
        for (Payment payment : payments) {
            reconcileOnePayment(payment).ifPresent(exception -> addIfNotAlreadyFlagged(exception, newExceptions));
        }
        return payments.size();
    }

    private Optional<PaymentReconciliationException> reconcileOnePayment(Payment payment) {
        RazorpayOrderService.RazorpayOrderSnapshot snapshot;
        try {
            snapshot = razorpayOrderService.fetchOrder(payment.getRazorpayOrderId());
        } catch (PaymentGatewayException e) {
            log.warn("Could not fetch Razorpay order {} for payment {}: {}", payment.getRazorpayOrderId(), payment.getId(), e.getMessage());
            return Optional.of(new PaymentReconciliationException(ReconciliationEntityType.PAYMENT, payment.getId(),
                    payment.getRazorpayOrderId(), ReconciliationMismatchType.MISSING_AT_GATEWAY,
                    payment.getStatus().name(), null, payment.getAmount(), null,
                    "Razorpay has no record of order " + payment.getRazorpayOrderId() + " (" + e.getMessage() + ")"));
        }

        boolean gatewayPaid = "paid".equalsIgnoreCase(snapshot.status());
        boolean dbPaid = payment.getStatus() == PaymentStatus.SUCCESS;
        if (gatewayPaid != dbPaid) {
            return Optional.of(new PaymentReconciliationException(ReconciliationEntityType.PAYMENT, payment.getId(),
                    payment.getRazorpayOrderId(), ReconciliationMismatchType.STATUS_MISMATCH,
                    payment.getStatus().name(), snapshot.status(), payment.getAmount(), snapshot.amountPaid(),
                    "DB says " + payment.getStatus() + " but Razorpay order status is " + snapshot.status()));
        }

        if (dbPaid) {
            BigDecimal gatewayAmount = snapshot.amountPaid() != null ? snapshot.amountPaid() : snapshot.amount();
            if (gatewayAmount != null && payment.getAmount().compareTo(gatewayAmount) != 0) {
                return Optional.of(new PaymentReconciliationException(ReconciliationEntityType.PAYMENT, payment.getId(),
                        payment.getRazorpayOrderId(), ReconciliationMismatchType.AMOUNT_MISMATCH,
                        payment.getStatus().name(), snapshot.status(), payment.getAmount(), gatewayAmount,
                        "DB amount " + payment.getAmount() + " does not match Razorpay amount " + gatewayAmount));
            }
        }
        return Optional.empty();
    }

    private int reconcilePayroll(Instant since, List<PaymentReconciliationException> newExceptions) {
        if (!razorpayPayoutClient.isEnabled()) {
            log.info("RazorpayX payouts not configured - skipping payroll reconciliation");
            return 0;
        }
        List<PayrollRecord> records = payrollRecordRepository.findByRazorpayPayoutIdIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(since);
        for (PayrollRecord record : records) {
            reconcileOnePayrollRecord(record).ifPresent(exception -> addIfNotAlreadyFlagged(exception, newExceptions));
        }
        return records.size();
    }

    private Optional<PaymentReconciliationException> reconcileOnePayrollRecord(PayrollRecord record) {
        RazorpayPayoutClient.PayoutSnapshot snapshot;
        try {
            snapshot = razorpayPayoutClient.fetchPayout(record.getRazorpayPayoutId());
        } catch (PaymentGatewayException e) {
            log.warn("Could not fetch Razorpay payout {} for payroll record {}: {}", record.getRazorpayPayoutId(), record.getId(), e.getMessage());
            return Optional.of(new PaymentReconciliationException(ReconciliationEntityType.PAYROLL_RECORD, record.getId(),
                    record.getRazorpayPayoutId(), ReconciliationMismatchType.MISSING_AT_GATEWAY,
                    record.getStatus().name(), null, record.getTotalPaid(), null,
                    "Razorpay has no record of payout " + record.getRazorpayPayoutId() + " (" + e.getMessage() + ")"));
        }

        String gatewayStatus = snapshot.status() == null ? "" : snapshot.status().toLowerCase(Locale.ROOT);
        boolean gatewaySucceeded = "processed".equals(gatewayStatus);
        boolean gatewayFailed = TERMINAL_FAILED_PAYOUT_STATUSES.contains(gatewayStatus);
        boolean dbPaid = record.getStatus() == PayrollStatus.PAID;

        if (dbPaid && gatewayFailed) {
            return Optional.of(new PaymentReconciliationException(ReconciliationEntityType.PAYROLL_RECORD, record.getId(),
                    record.getRazorpayPayoutId(), ReconciliationMismatchType.STATUS_MISMATCH,
                    record.getStatus().name(), snapshot.status(), record.getTotalPaid(), snapshot.amount(),
                    "DB says PAID but Razorpay payout status is " + snapshot.status()));
        }
        if (!dbPaid && gatewaySucceeded) {
            return Optional.of(new PaymentReconciliationException(ReconciliationEntityType.PAYROLL_RECORD, record.getId(),
                    record.getRazorpayPayoutId(), ReconciliationMismatchType.STATUS_MISMATCH,
                    record.getStatus().name(), snapshot.status(), record.getTotalPaid(), snapshot.amount(),
                    "DB says " + record.getStatus() + " but Razorpay payout status is processed"));
        }
        if (dbPaid && gatewaySucceeded && snapshot.amount() != null && record.getTotalPaid() != null
                && record.getTotalPaid().compareTo(snapshot.amount()) != 0) {
            return Optional.of(new PaymentReconciliationException(ReconciliationEntityType.PAYROLL_RECORD, record.getId(),
                    record.getRazorpayPayoutId(), ReconciliationMismatchType.AMOUNT_MISMATCH,
                    record.getStatus().name(), snapshot.status(), record.getTotalPaid(), snapshot.amount(),
                    "DB amount " + record.getTotalPaid() + " does not match Razorpay payout amount " + snapshot.amount()));
        }
        return Optional.empty();
    }

    /** Skips re-inserting (and re-notifying about) a mismatch that's already an open worklist row from a previous run. */
    private void addIfNotAlreadyFlagged(PaymentReconciliationException candidate, List<PaymentReconciliationException> newExceptions) {
        boolean alreadyOpen = exceptionRepository.existsByEntityTypeAndEntityIdAndMismatchTypeAndResolvedFalse(
                candidate.getEntityType(), candidate.getEntityId(), candidate.getMismatchType());
        if (alreadyOpen) {
            return;
        }
        newExceptions.add(exceptionRepository.save(candidate));
    }

    /** Same dispatcher every other alert in the system funnels through (see NotificationDispatchService) - no new notification mechanism. */
    private void notifyPlatformAdmins(List<PaymentReconciliationException> newExceptions) {
        String subject = newExceptions.size() + " payment reconciliation mismatch(es) found";
        StringBuilder body = new StringBuilder("Reconciliation against Razorpay found ").append(newExceptions.size())
                .append(" mismatch(es):\n");
        newExceptions.stream().limit(10).forEach(e -> body.append("- ")
                .append(e.getEntityType()).append(' ').append(e.getEntityId()).append(": ")
                .append(e.getMismatchType()).append(" - ").append(e.getDetails()).append('\n'));
        if (newExceptions.size() > 10) {
            body.append("...and ").append(newExceptions.size() - 10).append(" more - see /platform-admin/reconciliation-exceptions");
        }

        try {
            for (UserAccount admin : userAccountRepository
                    .findByRole(Role.PLATFORM_ADMIN, PageRequest.of(0, properties.getMaxAdminsNotified())).getContent()) {
                if (admin.isActive()) {
                    dispatchService.dispatch(admin.getId(), "payment.RECONCILIATION_MISMATCH", subject, body.toString());
                }
            }
        } catch (Exception e) {
            log.error("Could not notify platform admins of {} reconciliation exceptions", newExceptions.size(), e);
        }
    }
}
