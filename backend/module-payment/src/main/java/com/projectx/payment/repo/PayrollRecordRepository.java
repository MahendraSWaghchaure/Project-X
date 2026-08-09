package com.projectx.payment.repo;

import com.projectx.payment.domain.PayrollRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    List<PayrollRecord> findByDriverIdOrderByPeriodStartDesc(Long driverId);

    Optional<PayrollRecord> findByDriverIdAndPeriodStart(Long driverId, LocalDate periodStart);

    List<PayrollRecord> findByPeriodStart(LocalDate periodStart);
}
