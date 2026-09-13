package com.projectx.geo.repo;

import com.projectx.geo.domain.State;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<State, Long> {

    List<State> findByCountryIdOrderByNameAsc(Long countryId);
}
