package com.app.service;

import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.entities.Students;
import com.app.repo.StudentsRepository;

@Service
public class StudentsService {
	@Autowired
	private static StudentsRepository studentsrepo;
	
	
	public Students saveProfile(Students profile) {
		return studentsrepo.save(profile);
	}
	
	public static List<Students> getAllStudents() {
		// TODO Auto-generated method stub
		return studentsrepo.findAll();
	}
	
	// Update School and Class	
	//Also Give access to School for changes in it
	//Many To One
	 public Students updateStudent(Long id, String school, String studentClass) {
	        return studentsrepo.findById(id).map(student -> {
	            student.setStudentSchool(school);
	            student.setStudentClass(studentClass);
	            return studentsrepo.save(student);
	        }).orElseThrow(() -> new EntityNotFoundException("Student not found"));
	    }
	
	public void deleteProfile(Long id) {
		studentsrepo.deleteById(id);
	}
}
