package com.app.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entities.Admin;

public interface AdminRepository extends JpaRepository<Admin,Long>{

	Optional<Admin> findByEmail(String username);
	// Custom queries can be added if needed
}