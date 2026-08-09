package com.projectx.insurance.service;

import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.insurance.domain.InsurancePolicy;
import com.projectx.insurance.dto.CreateInsurancePolicyRequest;
import com.projectx.insurance.dto.InsurancePolicyResponse;
import com.projectx.insurance.repo.InsurancePolicyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InsurancePolicyService {

    private final InsurancePolicyRepository insurancePolicyRepository;

    public InsurancePolicyService(InsurancePolicyRepository insurancePolicyRepository) {
        this.insurancePolicyRepository = insurancePolicyRepository;
    }

    public InsurancePolicyResponse createPolicy(Long schoolId, CreateInsurancePolicyRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        if (insurancePolicyRepository.existsByPolicyNumberIgnoreCase(request.policyNumber())) {
            throw new ConflictException("Policy number already recorded: " + request.policyNumber());
        }
        InsurancePolicy policy = new InsurancePolicy(schoolId, request.subjectType(), request.subjectId(),
                request.provider(), request.policyNumber(), request.coverageAmount(), request.startDate(), request.endDate());
        return InsurancePolicyResponse.from(insurancePolicyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public List<InsurancePolicyResponse> listPolicies(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return insurancePolicyRepository.findBySchoolId(schoolId).stream().map(InsurancePolicyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InsurancePolicyResponse getPolicy(Long schoolId, Long policyId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return InsurancePolicyResponse.from(findWithinSchool(schoolId, policyId));
    }

    private InsurancePolicy findWithinSchool(Long schoolId, Long policyId) {
        InsurancePolicy policy = insurancePolicyRepository.findById(policyId)
                .orElseThrow(() -> ResourceNotFoundException.of("InsurancePolicy", policyId));
        if (!policy.getSchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("InsurancePolicy", policyId);
        }
        return policy;
    }
}
