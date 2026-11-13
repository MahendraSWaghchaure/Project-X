package com.projectx.payment.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.payment.dto.FeeInvoiceResponse;
import com.projectx.payment.service.FeeInvoiceService;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeeInvoiceController {

    private final FeeInvoiceService feeInvoiceService;

    public FeeInvoiceController(FeeInvoiceService feeInvoiceService) {
        this.feeInvoiceService = feeInvoiceService;
    }

    @GetMapping("/parents/me/fee-invoices")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<List<FeeInvoiceResponse>> listMine() {
        return ApiResponse.ok(feeInvoiceService.listMine());
    }

    @GetMapping("/schools/{schoolId}/fee-invoices")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PageResponse<FeeInvoiceResponse>> listForSchool(@PathVariable Long schoolId,
            @PageableDefault(size = 30) Pageable pageable) {
        return ApiResponse.ok(feeInvoiceService.listForSchool(schoolId, pageable));
    }
}
