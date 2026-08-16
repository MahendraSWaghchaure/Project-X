-- Supports the dead-letter reprocessing query (NotificationRecordRepository.findByStatus) without
-- a full table scan once notification_records grows large.
CREATE INDEX idx_notification_records_status ON notification_records (status);
