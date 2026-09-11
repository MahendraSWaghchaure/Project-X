package com.projectx.payment.repo;

import com.projectx.payment.domain.FeeInvoice;
import com.projectx.payment.domain.FeeInvoiceStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeInvoiceRepository extends JpaRepository<FeeInvoice, Long> {

    List<FeeInvoice> findByParentIdOrderByBillingMonthDesc(Long parentId);

    List<FeeInvoice> findByChildIdOrderByBillingMonthDesc(Long childId);

    List<FeeInvoice> findBySchoolIdOrderByBillingMonthDesc(Long schoolId);

    Optional<FeeInvoice> findByChildIdAndBillingMonth(Long childId, LocalDate billingMonth);

    List<FeeInvoice> findByStatusNotIn(List<FeeInvoiceStatus> statuses);

    boolean existsByChildIdAndBillingMonth(Long childId, LocalDate billingMonth);

    /** One query for "which of these children already have a {@code billingMonth} invoice" instead of one exists-check per child - see FeeInvoiceService.ensureInvoicesExistForMonth. */
    List<FeeInvoice> findByChildIdInAndBillingMonth(Collection<Long> childIds, LocalDate billingMonth);
}
