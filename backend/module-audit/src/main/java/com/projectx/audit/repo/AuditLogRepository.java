package com.projectx.audit.repo;

import com.projectx.audit.domain.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    Page<AuditLogEntry> findBySchoolIdOrderByOccurredAtDesc(Long schoolId, Pageable pageable);

    Page<AuditLogEntry> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
