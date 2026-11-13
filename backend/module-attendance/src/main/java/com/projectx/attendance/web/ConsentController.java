package com.projectx.attendance.web;

import com.projectx.attendance.dto.ConsentDtos.ConsentLinkResponse;
import com.projectx.attendance.dto.ConsentDtos.ConsentReceipt;
import com.projectx.attendance.dto.ConsentDtos.ConsentRespondRequest;
import com.projectx.attendance.dto.ConsentDtos.ConsentView;
import com.projectx.attendance.service.ConsentService;
import com.projectx.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guardian consent links. {@code POST /schools/{id}/students/{id}/consent-link} is for school / platform admins; the two
 * {@code /public/consent/**} endpoints are unauthenticated and rate limited (RateLimitFilter) - see ConsentService for the
 * token rules (48 h, single use, hashed at rest, uniform 410 on any failure).
 */
@RestController
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping("/schools/{schoolId}/students/{studentId}/consent-link")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<ConsentLinkResponse> sendLink(@PathVariable Long schoolId, @PathVariable Long studentId) {
        return ApiResponse.ok(consentService.requestLink(schoolId, studentId));
    }

    @GetMapping("/public/consent/{token}")
    public ApiResponse<ConsentView> view(@PathVariable String token) {
        return ApiResponse.ok(consentService.view(token));
    }

    @PostMapping("/public/consent/{token}/respond")
    public ApiResponse<ConsentReceipt> respond(@PathVariable String token, @Valid @RequestBody ConsentRespondRequest request) {
        return ApiResponse.ok(consentService.respond(token, request.consent()));
    }
}
