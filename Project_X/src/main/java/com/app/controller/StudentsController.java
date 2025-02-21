package com.app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.entities.Students;
import com.app.service.StudentsService;

@RestController
@RequestMapping("/api/students")
public class StudentsController {
	
	@Autowired
	private StudentsService studentProfileService;
	
	//Create Profile and all details in it
	@PostMapping
	public ResponseEntity<Students> createStudent(@RequestBody Students profile){
		Students savedStudents = studentProfileService.saveProfile(profile);
		return new ResponseEntity<>(savedStudents, HttpStatus.CREATED);
		}
	
	//Get List of Students
	@GetMapping("/details")
	public ResponseEntity<List<Students>> viewAllStudents(){
		List<Students> studentList = StudentsService.getAllStudents();
		return new ResponseEntity<>(HttpStatus.CREATED);
	}
	
	//Update School and Class of students
	@PutMapping("/{Id}")
	public ResponseEntity<Students> updateStudents(@PathVariable Long Id,@RequestBody Students request ){
		Students updatedStudent = studentProfileService.updateStudent(Id, request.getStudentSchool(), request.getStudentClass());
		return ResponseEntity.ok(updatedStudent);
	}
	
	//Delete Profile By putting in ID
	@DeleteMapping("/{Id}")
	public ResponseEntity<Void> deleteProfile (@PathVariable Long Id){
		studentProfileService.deleteProfile(Id);
		return ResponseEntity.noContent().build();
	}
	
}
