package com.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entities.Students;

public interface StudentsRepository extends JpaRepository<Students,Long>{
	
	
}
