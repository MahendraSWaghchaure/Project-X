package com.projectx.payment.repo;

import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.domain.PayrollStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    List<PayrollRecord> findByDriverIdOrderByPeriodStartDesc(Long driverId);

    Optional<PayrollRecord> findByDriverIdAndPeriodStart(Long driverId, LocalDate periodStart);

    List<PayrollRecord> findByPeriodStart(LocalDate periodStart);

    /** PayrollRecord has no schoolId column of its own (scoped by driverId, cross-module) - callers pass the bounded set of driver ids already fetched for one school. */
    long countByDriverIdInAndStatusIn(Collection<Long> driverIds, Collection<PayrollStatus> statuses);

    /** The newest payroll rows of a set of drivers, capped by the Pageable (never a full-table scan). */
    List<PayrollRecord> findByDriverIdIn(Collection<Long> driverIds, Pageable pageable);

    boolean existsByDriverIdAndStatus(Long driverId, PayrollStatus status);

    /** PaymentReconciliationService's working set - only records that actually went through a real RazorpayX payout call (razorpayPayoutId is only ever set by PayrollDisbursementService.disburseOne). */
    List<PayrollRecord> findByRazorpayPayoutIdIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(Instant after);
}
