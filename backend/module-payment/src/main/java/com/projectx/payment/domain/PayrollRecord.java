package com.projectx.payment.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data model only for now - driverId is a cross-module id reference (module-people), same
 * discipline as elsewhere. incentiveAmount is meant for the "covered another driver's route
 * during a breakdown" bonus from the product spec; the computation that fills these fields in
 * from actual overtime/cover-duty records is phase 2, same as Stripe billing.
 */
@Entity
@Table(name = "payroll_records")
public class PayrollRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    /** Always the 1st of the payroll month. */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "overtime_hours", precision = 6, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "incentive_amount", precision = 12, scale = 2)
    private BigDecimal incentiveAmount = BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 12, scale = 2)
    private BigDecimal totalPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayrollStatus status = PayrollStatus.PENDING;

    protected PayrollRecord() {
    }

    public PayrollRecord(Long driverId, LocalDate periodStart, BigDecimal baseSalary) {
        this.driverId = driverId;
        this.periodStart = periodStart;
        this.baseSalary = baseSalary;
    }

    public Long getId() {
        return id;
    }

    public Long getDriverId() {
        return driverId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public BigDecimal getIncentiveAmount() {
        return incentiveAmount;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public PayrollStatus getStatus() {
        return status;
    }
}
