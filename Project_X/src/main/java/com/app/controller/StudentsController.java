package com.app.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.entities.Students;
import com.app.service.StudentsService;

@RestController
@RequestMapping("/api/students")
public class StudentsController {
	
//	private static final Logger logger= LoggerFactory.getLogger(StudentsController.class);
	
	@Autowired
	private StudentsService studentProfileService;
	
	@GetMapping("/details")
	public ResponseEntity<List<Students>> viewAllStudents(){
		List<Students> studentList = StudentsService.getAllStudents();
		return new ResponseEntity<>(HttpStatus.CREATED);
	}
	
	
}
