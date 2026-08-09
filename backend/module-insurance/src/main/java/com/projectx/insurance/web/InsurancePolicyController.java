package com.projectx.insurance.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.insurance.dto.CreateInsurancePolicyRequest;
import com.projectx.insurance.dto.InsurancePolicyResponse;
import com.projectx.insurance.service.InsurancePolicyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schools/{schoolId}/insurance-policies")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
public class InsurancePolicyController {

    private final InsurancePolicyService insurancePolicyService;

    public InsurancePolicyController(InsurancePolicyService insurancePolicyService) {
        this.insurancePolicyService = insurancePolicyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InsurancePolicyResponse> createPolicy(@PathVariable Long schoolId,
                                                               @Valid @RequestBody CreateInsurancePolicyRequest request) {
        return ApiResponse.ok(insurancePolicyService.createPolicy(schoolId, request));
    }

    @GetMapping
    public ApiResponse<List<InsurancePolicyResponse>> listPolicies(@PathVariable Long schoolId) {
        return ApiResponse.ok(insurancePolicyService.listPolicies(schoolId));
    }

    @GetMapping("/{policyId}")
    public ApiResponse<InsurancePolicyResponse> getPolicy(@PathVariable Long schoolId, @PathVariable Long policyId) {
        return ApiResponse.ok(insurancePolicyService.getPolicy(schoolId, policyId));
    }
}
