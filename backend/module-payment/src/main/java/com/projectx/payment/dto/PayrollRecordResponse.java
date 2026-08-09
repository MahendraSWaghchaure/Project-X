package com.projectx.payment.dto;

import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.domain.PayrollStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PayrollRecordResponse(
        Long id, Long driverId, LocalDate periodStart, BigDecimal baseSalary, BigDecimal overtimeHours,
        BigDecimal incentiveAmount, BigDecimal totalPaid, PayrollStatus status, String transactionReference,
        Instant disbursedAt, String failureReason) {

    public static PayrollRecordResponse from(PayrollRecord record) {
        return new PayrollRecordResponse(record.getId(), record.getDriverId(), record.getPeriodStart(),
                record.getBaseSalary(), record.getOvertimeHours(), record.getIncentiveAmount(), record.getTotalPaid(),
                record.getStatus(), record.getTransactionReference(), record.getDisbursedAt(), record.getFailureReason());
    }
}
