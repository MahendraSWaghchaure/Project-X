package com.projectx.attendance.service;

import java.time.LocalDate;
import java.time.ZoneId;

/** A school day is an India-local calendar day (same zone DashboardStatsService uses), regardless of the server's own zone. */
public final class SchoolTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private SchoolTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
