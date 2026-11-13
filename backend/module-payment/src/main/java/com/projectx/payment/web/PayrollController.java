package com.projectx.payment.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.payment.dto.PayrollRecordResponse;
import com.projectx.payment.dto.SetIncentiveRequest;
import com.projectx.payment.service.PayrollService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/drivers/me/payroll")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<PayrollRecordResponse>> listMine() {
        return ApiResponse.ok(payrollService.listMine());
    }

    @GetMapping("/schools/{schoolId}/payroll")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<PayrollRecordResponse>> listForSchool(@PathVariable Long schoolId) {
        return ApiResponse.ok(payrollService.listForSchool(schoolId));
    }

    @PatchMapping("/schools/{schoolId}/payroll/drivers/{driverId}/incentive")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PayrollRecordResponse> setIncentive(@PathVariable Long schoolId, @PathVariable Long driverId,
                                                             @Valid @RequestBody SetIncentiveRequest request) {
        return ApiResponse.ok(payrollService.setIncentive(schoolId, driverId, request.incentiveAmount()));
    }
}
