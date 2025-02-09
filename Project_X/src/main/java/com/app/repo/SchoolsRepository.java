package com.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entities.Schools;

public interface SchoolsRepository extends JpaRepository<Schools, Long>{

}