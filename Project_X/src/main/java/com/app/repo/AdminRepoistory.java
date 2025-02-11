package com.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.entities.Admin;

public interface AdminRepoistory extends JpaRepository<Admin,Long>{

}