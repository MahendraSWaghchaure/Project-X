package com.projectx.payment.repo;

import com.projectx.payment.domain.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByParentIdOrderByCreatedAtDesc(Long parentId);

    /** Paginated variant for the parent-facing payment-history page. listForSchool (the admin dashboard's revenue aggregation) deliberately stays on the unbounded List below - it sums every payment client-side, so paginating it would silently under-report revenue. */
    Page<Payment> findByParentIdOrderByCreatedAtDesc(Long parentId, Pageable pageable);

    List<Payment> findBySchoolIdOrderByCreatedAtDesc(Long schoolId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    long countByReceiptNumberIsNotNull();
}
