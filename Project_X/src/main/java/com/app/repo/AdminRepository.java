package com.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entities.Admin;

public interface AdminRepository extends JpaRepository<Admin,Long>{
	// Custom queries can be added if needed
}