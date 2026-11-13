package com.projectx.payment.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.payment.domain.DriverBankDetail;
import com.projectx.payment.domain.PayrollStatus;
import com.projectx.payment.dto.BankDetailResponse;
import com.projectx.payment.dto.UpsertBankDetailsRequest;
import com.projectx.payment.repo.DriverBankDetailRepository;
import com.projectx.payment.repo.PayrollRecordRepository;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.repo.DriverProfileRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DriverBankDetailService {

    private final DriverBankDetailRepository bankDetailRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DriverBankDetailService(DriverBankDetailRepository bankDetailRepository, DriverProfileRepository driverProfileRepository,
                                   PayrollRecordRepository payrollRecordRepository, ApplicationEventPublisher eventPublisher) {
        this.bankDetailRepository = bankDetailRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.eventPublisher = eventPublisher;
    }

    public BankDetailResponse upsertMine(UpsertBankDetailsRequest request) {
        DriverProfile driver = currentDriver();
        if (bankDetailRepository.existsByAccountNumberAndDriverIdNot(request.accountNumber(), driver.getId())) {
            throw new ConflictException("This account number is already on file for another driver");
        }
        DriverBankDetail detail = bankDetailRepository.findByDriverId(driver.getId())
                .map(existing -> {
                    existing.updateDetails(request.accountHolderName(), request.accountNumber(), request.ifscCode());
                    return existing;
                })
                .orElseGet(() -> new DriverBankDetail(driver.getId(), request.accountHolderName(), request.accountNumber(), request.ifscCode()));
        return BankDetailResponse.from(bankDetailRepository.save(detail));
    }

    @Transactional(readOnly = true)
    public BankDetailResponse getMine() {
        DriverProfile driver = currentDriver();
        return bankDetailRepository.findByDriverId(driver.getId())
                .map(BankDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No bank details on file yet"));
    }

    /**
     * The driver removes their one bank record; afterwards GET returns 404 again and the add-form shows. Refused (409)
     * while a payout is in flight (PROCESSING) so the money cannot be sent to an account that was just deleted. The audit
     * event is published inside the transaction and never carries the account number.
     */
    public void deleteMine() {
        DriverProfile driver = currentDriver();
        DriverBankDetail detail = bankDetailRepository.findByDriverId(driver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No bank details on file yet"));
        if (payrollRecordRepository.existsByDriverIdAndStatus(driver.getId(), PayrollStatus.PROCESSING)) {
            throw new ConflictException("A salary payout is being processed - you can remove your bank details once it completes");
        }
        bankDetailRepository.delete(detail);
        eventPublisher.publishEvent(AuditEvent.of("DRIVER_BANK_DETAILS_DELETED", "DRIVER_PROFILE", driver.getId(),
                driver.getSchoolId(), "Bank details removed by the driver"));
    }

    private DriverProfile currentDriver() {
        CurrentUser user = CurrentUserContext.get();
        return driverProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> new ForbiddenOperationException("No driver profile for the current user"));
    }
}
