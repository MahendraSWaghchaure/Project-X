package com.projectx.payment.dto;

import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.domain.PayrollStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * {@code transactionId} is the payout gateway's reference (same value as {@code transactionReference}, kept for existing
 * clients). {@code payoutAmount} = the amount actually paid once PAID, otherwise the amount due (base salary + incentive).
 * The salary-history "date" is {@code disbursedAt} when paid, else {@code periodStart}.
 */
public record PayrollRecordResponse(
        Long id, Long driverId, LocalDate periodStart, BigDecimal baseSalary, BigDecimal overtimeHours,
        BigDecimal incentiveAmount, BigDecimal totalPaid, PayrollStatus status, String transactionReference,
        Instant disbursedAt, String failureReason, String transactionId, BigDecimal payoutAmount) {

    public static PayrollRecordResponse from(PayrollRecord record) {
        BigDecimal due = record.getBaseSalary() == null ? null
                : record.getBaseSalary().add(record.getIncentiveAmount() == null ? BigDecimal.ZERO : record.getIncentiveAmount());
        return new PayrollRecordResponse(record.getId(), record.getDriverId(), record.getPeriodStart(),
                record.getBaseSalary(), record.getOvertimeHours(), record.getIncentiveAmount(), record.getTotalPaid(),
                record.getStatus(), record.getTransactionReference(), record.getDisbursedAt(), record.getFailureReason(),
                record.getTransactionReference(), record.getTotalPaid() != null ? record.getTotalPaid() : due);
    }
}
