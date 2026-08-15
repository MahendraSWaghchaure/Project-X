package com.projectx.payment.service;

import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ChildStatus;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.payment.domain.FeeInvoice;
import com.projectx.payment.domain.FeeInvoiceStatus;
import com.projectx.payment.domain.FeeSchedule;
import com.projectx.payment.dto.FeeInvoiceResponse;
import com.projectx.payment.repo.FeeInvoiceRepository;
import com.projectx.payment.repo.FeeScheduleRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns fee_invoices' lifecycle: monthly generation, daily late-fee accrual (both driven by
 * BillingScheduler), and on-demand generation of future months for the advance-payment flow
 * (see PaymentService.createAdvanceOrder).
 */
@Service
@Transactional
public class FeeInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(FeeInvoiceService.class);
    /** Due on the 7th of the billing month - see the product spec ("payment should be done before 7th of each month"). */
    private static final int DUE_DAY_OF_MONTH = 7;

    private final FeeInvoiceRepository feeInvoiceRepository;
    private final FeeScheduleRepository feeScheduleRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;

    public FeeInvoiceService(FeeInvoiceRepository feeInvoiceRepository, FeeScheduleRepository feeScheduleRepository,
                              ChildRepository childRepository, ParentProfileRepository parentProfileRepository) {
        this.feeInvoiceRepository = feeInvoiceRepository;
        this.feeScheduleRepository = feeScheduleRepository;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
    }

    /** Ensures every active child has an invoice for the given billing month, creating any that are missing. Idempotent. */
    public void ensureInvoicesExistForMonth(YearMonth billingMonth) {
        LocalDate monthStart = billingMonth.atDay(1);
        LocalDate dueDate = billingMonth.atDay(Math.min(DUE_DAY_OF_MONTH, billingMonth.lengthOfMonth()));

        for (FeeSchedule schedule : feeScheduleRepository.findAll()) {
            List<Child> activeChildren = childRepository.findBySchoolId(schedule.getSchoolId()).stream()
                    .filter(c -> c.getStatus() == ChildStatus.ACTIVE)
                    .toList();
            for (Child child : activeChildren) {
                if (feeInvoiceRepository.existsByChildIdAndBillingMonth(child.getId(), monthStart)) {
                    continue;
                }
                FeeInvoice invoice = new FeeInvoice(child.getId(), child.getPrimaryParentId(), child.getSchoolId(),
                        monthStart, schedule.getMonthlyAmount(), dueDate);
                feeInvoiceRepository.save(invoice);
                log.info("Generated fee invoice for child {} for {}", child.getId(), monthStart);
            }
        }
    }

    /**
     * The advance-payment building block: makes sure the next `months` not-yet-billed months for
     * this child exist (creating them if needed, at the school's current fee schedule rate), and
     * returns all of them so PaymentService can total them up into one order.
     */
    List<FeeInvoice> ensureAdvanceInvoices(Child child, int months) {
        FeeSchedule schedule = feeScheduleRepository.findBySchoolId(child.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("This school hasn't configured a fee schedule yet"));

        YearMonth startMonth = nextUnbilledMonth(child.getId());
        return IntStream.range(0, months)
                .mapToObj(startMonth::plusMonths)
                .map(billingMonth -> feeInvoiceRepository.findByChildIdAndBillingMonth(child.getId(), billingMonth.atDay(1))
                        .orElseGet(() -> {
                            LocalDate dueDate = billingMonth.atDay(Math.min(DUE_DAY_OF_MONTH, billingMonth.lengthOfMonth()));
                            FeeInvoice invoice = new FeeInvoice(child.getId(), child.getPrimaryParentId(), child.getSchoolId(),
                                    billingMonth.atDay(1), schedule.getMonthlyAmount(), dueDate);
                            return feeInvoiceRepository.save(invoice);
                        }))
                .toList();
    }

    /**
     * The next month that still needs to be paid: one past the latest *paid* invoice, or the
     * current month if nothing's paid yet (or the latest paid month is already in the past).
     * Deliberately keyed off PAID rather than "latest existing invoice" - an abandoned or
     * not-yet-verified advance-payment attempt already creates unpaid invoice rows further out,
     * and if this looked at those instead, every repeated (abandoned) attempt would push the
     * start month further out again, piling up orphaned unpaid invoices instead of reusing the
     * ones already sitting there unpaid.
     */
    private YearMonth nextUnbilledMonth(Long childId) {
        YearMonth currentMonth = YearMonth.now();
        return feeInvoiceRepository.findByChildIdOrderByBillingMonthDesc(childId).stream()
                .filter(i -> i.getStatus() == FeeInvoiceStatus.PAID)
                .findFirst()
                .map(i -> YearMonth.from(i.getBillingMonth()))
                .filter(latest -> !latest.isBefore(currentMonth))
                .map(latest -> latest.plusMonths(1))
                .orElse(currentMonth);
    }

    /** Runs once a day; adds the flat late fee to every unpaid invoice that's now overdue and wasn't already charged today. */
    public void accrueLateFeesForToday() {
        LocalDate today = LocalDate.now();
        List<FeeInvoice> candidates = feeInvoiceRepository.findByStatusNotIn(
                List.of(FeeInvoiceStatus.PAID, FeeInvoiceStatus.CANCELLED));
        long accrued = candidates.stream()
                .filter(invoice -> !invoice.isLateFeeWaived() && today.isAfter(invoice.getDueDate())
                        && !today.equals(invoice.getLastLateFeeAccrualDate()))
                .peek(invoice -> invoice.accrueLateFeeForToday(today))
                .count();
        log.info("Late fee accrual run for {}: {} invoice(s) charged", today, accrued);
    }

    @Transactional(readOnly = true)
    public List<FeeInvoiceResponse> listMine() {
        ParentProfile parent = currentParent();
        return feeInvoiceRepository.findByParentIdOrderByBillingMonthDesc(parent.getId()).stream()
                .map(FeeInvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FeeInvoiceResponse> listForSchool(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return feeInvoiceRepository.findBySchoolIdOrderByBillingMonthDesc(schoolId).stream()
                .map(FeeInvoiceResponse::from).toList();
    }

    private ParentProfile currentParent() {
        CurrentUser user = CurrentUserContext.get();
        return parentProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for the current user"));
    }
}
