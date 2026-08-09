package com.projectx.insurance.repo;

import com.projectx.insurance.domain.InsurancePolicy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {

    List<InsurancePolicy> findBySchoolId(Long schoolId);

    boolean existsByPolicyNumberIgnoreCase(String policyNumber);
}
