package com.projectx.payment.repo;

import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.domain.PayrollStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    List<PayrollRecord> findByDriverIdOrderByPeriodStartDesc(Long driverId);

    Optional<PayrollRecord> findByDriverIdAndPeriodStart(Long driverId, LocalDate periodStart);

    List<PayrollRecord> findByPeriodStart(LocalDate periodStart);

    /** PayrollRecord has no schoolId column of its own (scoped by driverId, cross-module) - callers pass the bounded set of driver ids already fetched for one school. */
    long countByDriverIdInAndStatusIn(Collection<Long> driverIds, Collection<PayrollStatus> statuses);
}
