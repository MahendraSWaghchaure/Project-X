package com.projectx.payment.service;

import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.DriverProfile;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.payment.domain.DriverBankDetail;
import com.projectx.payment.dto.BankDetailResponse;
import com.projectx.payment.dto.UpsertBankDetailsRequest;
import com.projectx.payment.repo.DriverBankDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DriverBankDetailService {

    private final DriverBankDetailRepository bankDetailRepository;
    private final DriverProfileRepository driverProfileRepository;

    public DriverBankDetailService(DriverBankDetailRepository bankDetailRepository, DriverProfileRepository driverProfileRepository) {
        this.bankDetailRepository = bankDetailRepository;
        this.driverProfileRepository = driverProfileRepository;
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

    private DriverProfile currentDriver() {
        CurrentUser user = CurrentUserContext.get();
        return driverProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> new ForbiddenOperationException("No driver profile for the current user"));
    }
}
