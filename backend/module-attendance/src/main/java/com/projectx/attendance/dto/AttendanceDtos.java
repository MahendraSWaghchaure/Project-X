package com.projectx.attendance.dto;

import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.domain.Subscription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {

    /** Hard cap on one bulk write / one sheet read - a classroom is tens of rows; this is abuse protection, not a feature limit. */
    public static final int MAX_BULK_ENTRIES = 500;

    private AttendanceDtos() {
    }

    public record SheetRow(Long studentId, String name, String rollNo, Subscription subscription,
                           AttendanceStatus status, ArrivalOutcome arrival) {
    }

    public record BulkEntry(@NotNull Long studentId, @NotNull AttendanceStatus status) {
    }

    public record BulkAttendanceRequest(
            @NotNull LocalDate date,
            @NotNull Long classroomId,
            @NotEmpty @Size(max = MAX_BULK_ENTRIES) List<@Valid @NotNull BulkEntry> entries) {
    }

    /** unchanged = rows that already had exactly this status (re-sending the same sheet is a no-op write). */
    public record BulkAttendanceResult(LocalDate date, Long classroomId, int total, int inserted, int updated, int unchanged) {
    }

    public record AttendanceSummary(long total, long present, long absent, long late, long unmarked, long subscribed,
                                    long nonSubscribed, long mismatches) {
    }
}
