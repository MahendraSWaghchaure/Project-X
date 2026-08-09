package com.projectx.payment.service;

import com.projectx.payment.domain.DriverBankDetail;
import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.domain.PayrollStatus;
import com.projectx.payment.gateway.PaymentGatewayException;
import com.projectx.payment.gateway.RazorpayPayoutClient;
import com.projectx.payment.repo.DriverBankDetailRepository;
import com.projectx.payment.repo.PayrollRecordRepository;
import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.repo.DriverProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs driver salary disbursement on the 10th of each month (see BillingScheduler). Real money
 * only moves if RazorpayX payouts are actually configured (see RazorpayProperties) - otherwise
 * every record is honestly marked FAILED with that reason rather than pretending to succeed, since
 * this is the system of record parents and drivers both look at.
 */
@Service
@Transactional
public class PayrollDisbursementService {

    private static final Logger log = LoggerFactory.getLogger(PayrollDisbursementService.class);

    private final PayrollRecordRepository payrollRecordRepository;
    private final DriverBankDetailRepository bankDetailRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final RazorpayPayoutClient payoutClient;

    public PayrollDisbursementService(PayrollRecordRepository payrollRecordRepository,
                                       DriverBankDetailRepository bankDetailRepository,
                                       DriverProfileRepository driverProfileRepository,
                                       RazorpayPayoutClient payoutClient) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.bankDetailRepository = bankDetailRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.payoutClient = payoutClient;
    }

    /** Idempotent - a driver already PAID for this period is left alone if the run happens more than once. */
    public void disburseForMonth(LocalDate periodStart) {
        List<DriverProfile> employedDrivers = driverProfileRepository.findAll().stream()
                .filter(driver -> driver.getSchoolId() != null)
                .filter(driver -> driver.getEmploymentStatus() == DriverEmploymentStatus.ACTIVE)
                .filter(driver -> driver.getBaseSalaryMonthly() != null)
                .toList();

        int paid = 0;
        int failed = 0;
        for (DriverProfile driver : employedDrivers) {
            PayrollRecord record = payrollRecordRepository.findByDriverIdAndPeriodStart(driver.getId(), periodStart)
                    .orElseGet(() -> new PayrollRecord(driver.getId(), periodStart, driver.getBaseSalaryMonthly()));
            if (record.getStatus() == PayrollStatus.PAID) {
                continue;
            }
            record.markProcessing();
            record = payrollRecordRepository.save(record);

            try {
                disburseOne(driver, record);
                paid++;
            } catch (RuntimeException e) {
                record.markFailed(e.getMessage());
                log.error("Payroll disbursement failed for driver {} ({}): {}", driver.getId(), periodStart, e.getMessage());
                failed++;
            }
        }
        log.info("Payroll run for {}: {} disbursed, {} failed", periodStart, paid, failed);
    }

    public void disburseForCurrentMonth() {
        disburseForMonth(LocalDate.now().withDayOfMonth(1));
    }

    private void disburseOne(DriverProfile driver, PayrollRecord record) {
        DriverBankDetail bankDetail = bankDetailRepository.findByDriverId(driver.getId())
                .orElseThrow(() -> new PaymentGatewayException("No bank details on file for this driver"));

        if (bankDetail.getRazorpayFundAccountId() == null) {
            String contactId = bankDetail.getRazorpayContactId() != null
                    ? bankDetail.getRazorpayContactId()
                    : payoutClient.createContact(driver.getFirstName() + " " + driver.getLastName(), "driver-" + driver.getId());
            String fundAccountId = payoutClient.createFundAccount(contactId, bankDetail.getAccountHolderName(),
                    bankDetail.getAccountNumber(), bankDetail.getIfscCode());
            bankDetail.cacheRazorpayIds(contactId, fundAccountId);
        }

        // Overtime hours have no currency rate defined anywhere in the system yet (same gap the
        // original payroll stub flagged) - only base salary and any manually-set incentive amount
        // (the "covered another driver's route" bonus from the spec) are actually disbursed.
        BigDecimal totalPaid = record.getBaseSalary().add(record.getIncentiveAmount());
        String referenceId = "payroll-" + record.getId();
        String narration = "Salary " + record.getPeriodStart();

        RazorpayPayoutClient.PayoutResult result = payoutClient.createPayout(
                bankDetail.getRazorpayFundAccountId(), totalPaid, referenceId, narration);
        record.markPaid(totalPaid, result.transactionReference());
    }
}
