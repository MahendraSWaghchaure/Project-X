package com.projectx.geo.repo;

import com.projectx.geo.domain.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findByStateIdOrderByNameAsc(Long stateId);
}
