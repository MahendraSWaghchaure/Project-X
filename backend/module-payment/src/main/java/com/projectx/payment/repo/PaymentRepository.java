package com.projectx.payment.repo;

import com.projectx.payment.domain.Payment;
import com.projectx.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByParentIdOrderByCreatedAtDesc(Long parentId);

    /** Paginated variant for the parent-facing payment-history page. listForSchool below still feeds the school-admin Payments page's own full listing/browse table - a separate, still-legitimate unbounded use from the dashboard's revenue figure, which now comes from the indexed sum query below instead of summing this list client-side. */
    Page<Payment> findByParentIdOrderByCreatedAtDesc(Long parentId, Pageable pageable);

    List<Payment> findBySchoolIdOrderByCreatedAtDesc(Long schoolId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    long countByReceiptNumberIsNotNull();

    /** Powers the dashboard's "revenue this month" stat via an indexed DB-side sum, instead of fetching every payment row to add them up in application code. */
    @Query("select coalesce(sum(p.amount), 0) from Payment p "
            + "where p.schoolId = :schoolId and p.status = :status and p.createdAt >= :from and p.createdAt < :to")
    BigDecimal sumAmountForSchoolByStatusBetween(@Param("schoolId") Long schoolId, @Param("status") PaymentStatus status,
            @Param("from") Instant from, @Param("to") Instant to);
}
