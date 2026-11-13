package com.projectx.notification.repo;

import com.projectx.notification.domain.Inquiry;
import com.projectx.notification.domain.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByStatus(InquiryStatus status, Pageable pageable);

    long countByStatus(InquiryStatus status);
}
