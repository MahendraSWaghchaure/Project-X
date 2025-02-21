package com.app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.entities.Schools;
import com.app.service.SchoolsService;

@RestController
@RequestMapping("/api/schools")
public class SchoolsController {
	
	@Autowired
	private SchoolsService schoolService;
	
	@PostMapping
	public ResponseEntity<Schools> createStudent(@RequestBody Schools profile){
		Schools savedSchools = schoolService.createProfile(profile);
		return new ResponseEntity<>(savedSchools, HttpStatus.CREATED);
		}
	
	//Get,Put, delete
	@GetMapping("/list")
	public ResponseEntity<List<Schools>> viewAllSchools(){
		List<Schools> schoolList = SchoolsService.GetAllSchools();
		return new ResponseEntity<>(HttpStatus.CREATED);
	}
	
	
	
	@DeleteMapping("/{Id}")
	public ResponseEntity<Void> deleteSchoolProfile(@PathVariable Long Id){
		schoolService.deleteSchoolProfile(Id);
		return ResponseEntity.noContent().build();
	}
	
}
