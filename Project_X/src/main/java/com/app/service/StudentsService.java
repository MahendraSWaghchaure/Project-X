package com.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.repo.StudentsRepository;

@Service
public class StudentsService {
	@Autowired
	private StudentsRepository studentsrepo;
	
//	public StudentsService saveProfile(StudentsService profile) {
//		return studentsrepo .save(profile);
//	}
	
}
