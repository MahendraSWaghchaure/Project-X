package com.projectx.app.repo;

import com.projectx.app.domain.Partner;
import com.projectx.app.domain.PartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    Page<Partner> findByType(PartnerType type, Pageable pageable);
}
