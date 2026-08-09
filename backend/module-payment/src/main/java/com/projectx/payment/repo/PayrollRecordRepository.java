package com.projectx.payment.repo;

import com.projectx.payment.domain.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/** No service/controller layer yet - see module-payment's pom.xml description. */
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
}
