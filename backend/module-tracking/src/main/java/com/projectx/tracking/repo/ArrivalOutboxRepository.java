package com.projectx.tracking.repo;

import com.projectx.tracking.domain.ArrivalOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArrivalOutboxRepository extends JpaRepository<ArrivalOutbox, Long> {
}
