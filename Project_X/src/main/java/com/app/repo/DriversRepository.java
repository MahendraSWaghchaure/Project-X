package com.app.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entities.Drivers;

public interface DriversRepository extends JpaRepository<Drivers,Long>{

	Optional<Drivers> findByVehicleNo(String vehicleNo);

	Optional<Drivers> findByUserName(String username);

	List<Drivers> findByName(String name);

	List<Drivers> findBySchool(String school);

	List<Drivers> findBydFirstName_(String name);

}
