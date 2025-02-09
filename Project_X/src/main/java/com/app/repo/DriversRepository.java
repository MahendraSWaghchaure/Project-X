package com.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entities.Drivers;

public interface DriversRepository extends JpaRepository<Drivers,Long>{

}