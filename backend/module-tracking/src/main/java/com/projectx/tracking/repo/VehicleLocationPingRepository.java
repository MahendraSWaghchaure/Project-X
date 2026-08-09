package com.projectx.tracking.repo;

import com.projectx.tracking.domain.VehicleLocationPing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleLocationPingRepository extends JpaRepository<VehicleLocationPing, Long> {
}
