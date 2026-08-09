package com.projectx.payment.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.payment.dto.CreateWaiverRequest;
import com.projectx.payment.dto.ReviewWaiverRequest;
import com.projectx.payment.dto.WaiverResponse;
import com.projectx.payment.service.LateFeeWaiverService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LateFeeWaiverController {

    private final LateFeeWaiverService waiverService;

    public LateFeeWaiverController(LateFeeWaiverService waiverService) {
        this.waiverService = waiverService;
    }

    @PostMapping("/parents/me/late-fee-waivers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<WaiverResponse> create(@Valid @RequestBody CreateWaiverRequest request) {
        return ApiResponse.ok(waiverService.create(request));
    }

    @GetMapping("/parents/me/late-fee-waivers")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<List<WaiverResponse>> listMine() {
        return ApiResponse.ok(waiverService.listMine());
    }

    /** Platform admin only - see LateFeeWaiverService's class javadoc. */
    @GetMapping("/late-fee-waivers")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<List<WaiverResponse>> listAll() {
        return ApiResponse.ok(waiverService.listAll());
    }

    @PatchMapping("/late-fee-waivers/{waiverId}/review")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<WaiverResponse> review(@PathVariable Long waiverId, @Valid @RequestBody ReviewWaiverRequest request) {
        return ApiResponse.ok(waiverService.review(waiverId, request));
    }
}
