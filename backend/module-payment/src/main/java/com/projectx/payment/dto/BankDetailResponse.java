package com.projectx.payment.dto;

import com.projectx.payment.domain.DriverBankDetail;

public record BankDetailResponse(Long id, Long driverId, String accountHolderName, String maskedAccountNumber, String ifscCode) {

    public static BankDetailResponse from(DriverBankDetail detail) {
        return new BankDetailResponse(detail.getId(), detail.getDriverId(), detail.getAccountHolderName(),
                mask(detail.getAccountNumber()), detail.getIfscCode());
    }

    private static String mask(String accountNumber) {
        if (accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }
}
