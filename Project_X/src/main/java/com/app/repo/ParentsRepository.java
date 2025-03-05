package com.app.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entities.Parents;

public interface ParentsRepository extends JpaRepository<Parents,Long>{

	Optional<Parents> findByPusername(String username);

}
