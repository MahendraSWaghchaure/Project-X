package com.projectx.payment.repo;

import com.projectx.payment.domain.LateFeeWaiverRequest;
import com.projectx.payment.domain.WaiverStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LateFeeWaiverRequestRepository extends JpaRepository<LateFeeWaiverRequest, Long> {

    List<LateFeeWaiverRequest> findAllByOrderByCreatedAtDesc();

    List<LateFeeWaiverRequest> findByParentIdOrderByCreatedAtDesc(Long parentId);

    Optional<LateFeeWaiverRequest> findByFeeInvoiceIdAndStatus(Long feeInvoiceId, WaiverStatus status);

    long countByStatus(WaiverStatus status);
}
