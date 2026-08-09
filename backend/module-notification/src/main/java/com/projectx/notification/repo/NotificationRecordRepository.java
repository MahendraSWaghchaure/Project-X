package com.projectx.notification.repo;

import com.projectx.notification.domain.NotificationRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
}
