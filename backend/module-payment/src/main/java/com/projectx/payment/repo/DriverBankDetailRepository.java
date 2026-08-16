package com.projectx.payment.repo;

import com.projectx.payment.domain.DriverBankDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverBankDetailRepository extends JpaRepository<DriverBankDetail, Long> {

    Optional<DriverBankDetail> findByDriverId(Long driverId);

    boolean existsByAccountNumberAndDriverIdNot(String accountNumber, Long driverId);
}
