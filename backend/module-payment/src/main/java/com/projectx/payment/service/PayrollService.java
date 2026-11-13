package com.projectx.payment.service;

import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.dto.PayrollRecordResponse;
import com.projectx.payment.repo.PayrollRecordRepository;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.repo.DriverProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PayrollService {

    static final int MAX_PAYROLL_LISTED = 500;

    private final PayrollRecordRepository payrollRecordRepository;
    private final DriverProfileRepository driverProfileRepository;

    public PayrollService(PayrollRecordRepository payrollRecordRepository, DriverProfileRepository driverProfileRepository) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<PayrollRecordResponse> listMine() {
        DriverProfile driver = currentDriver();
        return payrollRecordRepository.findByDriverIdOrderByPeriodStartDesc(driver.getId()).stream()
                .map(PayrollRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollRecordResponse> listForSchool(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Set<Long> driverIds = driverProfileRepository.findBySchoolId(schoolId).stream()
                .map(DriverProfile::getId).collect(Collectors.toSet());
        if (driverIds.isEmpty()) {
            return List.of();
        }
        // Newest 500 rows of this school's drivers, in SQL - never a full-table scan of every school's payroll.
        return payrollRecordRepository.findByDriverIdIn(driverIds,
                        PageRequest.of(0, MAX_PAYROLL_LISTED, Sort.by(Sort.Order.desc("periodStart"), Sort.Order.desc("id"))))
                .stream().map(PayrollRecordResponse::from).toList();
    }

    /** School admin sets the "covered another driver's route during a breakdown" bonus before the month's payroll run. */
    public PayrollRecordResponse setIncentive(Long schoolId, Long driverId, BigDecimal incentiveAmount) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        LocalDate currentPeriod = LocalDate.now().withDayOfMonth(1);
        DriverProfile driver = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> ResourceNotFoundException.of("Driver", driverId));
        if (!schoolId.equals(driver.getSchoolId())) {
            throw new ForbiddenOperationException("Driver does not belong to this school");
        }
        PayrollRecord record = payrollRecordRepository.findByDriverIdAndPeriodStart(driverId, currentPeriod)
                .orElseGet(() -> new PayrollRecord(driverId, currentPeriod, driver.getBaseSalaryMonthly()));
        record.setIncentiveAmount(incentiveAmount);
        return PayrollRecordResponse.from(payrollRecordRepository.save(record));
    }

    private DriverProfile currentDriver() {
        CurrentUser user = CurrentUserContext.get();
        return driverProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> new ForbiddenOperationException("No driver profile for the current user"));
    }
}
