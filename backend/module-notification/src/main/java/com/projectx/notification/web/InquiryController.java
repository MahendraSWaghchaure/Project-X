package com.projectx.notification.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.notification.domain.InquiryStatus;
import com.projectx.notification.dto.InquiryDtos.CreateInquiryRequest;
import com.projectx.notification.dto.InquiryDtos.InquiryReceipt;
import com.projectx.notification.dto.InquiryDtos.InquiryResponse;
import com.projectx.notification.dto.InquiryDtos.UpdateInquiryStatusRequest;
import com.projectx.notification.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /public/inquiries} is unauthenticated and rate limited (RateLimitFilter); the list/status endpoints are platform-admin only. */
@RestController
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping("/public/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InquiryReceipt> submit(@Valid @RequestBody CreateInquiryRequest request) {
        return ApiResponse.ok(inquiryService.submit(request));
    }

    @GetMapping("/platform-admin/inquiries")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PageResponse<InquiryResponse>> list(@RequestParam(required = false) InquiryStatus status,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(inquiryService.list(status, page, size));
    }

    @PatchMapping("/platform-admin/inquiries/{inquiryId}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<InquiryResponse> updateStatus(@PathVariable Long inquiryId, @Valid @RequestBody UpdateInquiryStatusRequest request) {
        return ApiResponse.ok(inquiryService.updateStatus(inquiryId, request.status()));
    }
}
