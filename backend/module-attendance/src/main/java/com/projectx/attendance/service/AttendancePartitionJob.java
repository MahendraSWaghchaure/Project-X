package com.projectx.attendance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the next months of school_attendance partitions ahead of the calendar so writes never fall
 * into the DEFAULT partition (which would work, but unpruned). Idempotent (CREATE TABLE IF NOT
 * EXISTS inside ensure_school_attendance_partitions). Runs on every instance; concurrent runs are
 * harmless. If a month already has rows stranded in the default partition Postgres refuses to
 * create it - logged loudly, needs a manual move of those rows (see ARCHITECTURE.md).
 */
@Component
public class AttendancePartitionJob {

    private static final Logger log = LoggerFactory.getLogger(AttendancePartitionJob.class);

    private final JdbcTemplate jdbc;

    public AttendancePartitionJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "${projectx.attendance.partition-cron:0 15 2 * * *}", zone = "Asia/Kolkata")
    public void ensurePartitions() {
        try {
            jdbc.execute("SELECT ensure_school_attendance_partitions(0, 6)");
        } catch (Exception e) {
            log.error("Could not ensure school_attendance partitions - writes will land in the default partition", e);
        }
    }
}
