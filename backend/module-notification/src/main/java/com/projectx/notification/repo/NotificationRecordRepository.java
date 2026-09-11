package com.projectx.notification.repo;

import com.projectx.notification.domain.NotificationRecord;
import com.projectx.notification.domain.NotificationStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    /** Paginated variant for the parent-facing notification feed, which only ever grows over an account's lifetime. */
    Page<NotificationRecord> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    List<NotificationRecord> findByStatus(NotificationStatus status);
}
