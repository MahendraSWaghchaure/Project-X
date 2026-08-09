package com.projectx.payment.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.payment.dto.FeeScheduleResponse;
import com.projectx.payment.dto.UpsertFeeScheduleRequest;
import com.projectx.payment.service.FeeScheduleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
public class FeeScheduleController {

    private final FeeScheduleService feeScheduleService;

    public FeeScheduleController(FeeScheduleService feeScheduleService) {
        this.feeScheduleService = feeScheduleService;
    }

    @PutMapping("/schools/{schoolId}/fee-schedule")
    public ApiResponse<FeeScheduleResponse> upsert(@PathVariable Long schoolId, @Valid @RequestBody UpsertFeeScheduleRequest request) {
        return ApiResponse.ok(feeScheduleService.upsert(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/fee-schedule")
    public ApiResponse<FeeScheduleResponse> get(@PathVariable Long schoolId) {
        return ApiResponse.ok(feeScheduleService.get(schoolId));
    }
}
