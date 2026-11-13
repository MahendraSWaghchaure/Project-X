package com.projectx.payment.repo;

import com.projectx.payment.domain.PaymentReconciliationException;
import com.projectx.payment.domain.ReconciliationEntityType;
import com.projectx.payment.domain.ReconciliationMismatchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentReconciliationExceptionRepository extends JpaRepository<PaymentReconciliationException, Long> {

    /** Paginated listing for GET /platform-admin/reconciliation-exceptions, newest first. */
    Page<PaymentReconciliationException> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Lets a daily rerun skip re-flagging (and re-notifying about) a mismatch that's already an open worklist item. */
    boolean existsByEntityTypeAndEntityIdAndMismatchTypeAndResolvedFalse(
            ReconciliationEntityType entityType, Long entityId, ReconciliationMismatchType mismatchType);
}
