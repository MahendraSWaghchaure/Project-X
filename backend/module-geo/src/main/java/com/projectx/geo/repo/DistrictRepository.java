package com.projectx.geo.repo;

import com.projectx.geo.domain.District;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, Long> {

    List<District> findByStateIdOrderByNameAsc(Long stateId);

    List<District> findByRegionIdOrderByNameAsc(Long regionId);

    /** Backs the "search a district by name and auto-fill its parent filters" flow - a plain substring match is enough for a reference-data table sized in the hundreds, not millions, of rows. */
    List<District> findTop20ByNameContainingIgnoreCaseOrderByNameAsc(String query);
}
