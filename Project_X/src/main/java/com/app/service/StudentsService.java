package com.app.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.entities.Students;
import com.app.repo.StudentsRepository;

@Service
public class StudentsService {
	@Autowired
	private static  StudentsRepository studentsrepo;
	
//	public StudentsService saveProfile(StudentsService profile) {
//		return studentsrepo .save(profile);
//	}
	
	public static List<Students> getAllStudents() {
		// TODO Auto-generated method stub
		return studentsrepo.findAll();
	}
}
