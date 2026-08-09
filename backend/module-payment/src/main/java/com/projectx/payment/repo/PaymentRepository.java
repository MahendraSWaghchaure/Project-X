package com.projectx.payment.repo;

import com.projectx.payment.domain.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByParentIdOrderByCreatedAtDesc(Long parentId);

    List<Payment> findBySchoolIdOrderByCreatedAtDesc(Long schoolId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    long countByReceiptNumberIsNotNull();
}
