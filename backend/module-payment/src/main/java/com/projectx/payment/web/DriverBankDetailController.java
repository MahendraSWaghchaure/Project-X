package com.projectx.payment.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.payment.dto.BankDetailResponse;
import com.projectx.payment.dto.UpsertBankDetailsRequest;
import com.projectx.payment.service.DriverBankDetailService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('DRIVER')")
public class DriverBankDetailController {

    private final DriverBankDetailService bankDetailService;

    public DriverBankDetailController(DriverBankDetailService bankDetailService) {
        this.bankDetailService = bankDetailService;
    }

    @PutMapping("/drivers/me/bank-details")
    public ApiResponse<BankDetailResponse> upsert(@Valid @RequestBody UpsertBankDetailsRequest request) {
        return ApiResponse.ok(bankDetailService.upsertMine(request));
    }

    @GetMapping("/drivers/me/bank-details")
    public ApiResponse<BankDetailResponse> getMine() {
        return ApiResponse.ok(bankDetailService.getMine());
    }
}
