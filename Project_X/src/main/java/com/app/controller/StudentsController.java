package com.app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.entities.Students;
import com.app.repo.StudentsRepository;
import com.app.service.StudentsService;

@RestController
@RequestMapping("/api/students")
public class StudentsController {
	
//	private static final Logger logger= LoggerFactory.getLogger(StudentsController.class);
	
	@Autowired
	private StudentsService studentProfileService;
	
	@Autowired
    private StudentsRepository studentsrepo;
	
	@GetMapping("/details")
	public ResponseEntity<List<Students>> viewAllStudents(){
		List<Students> studentList = StudentsService.getAllStudents();
		return new ResponseEntity<>(HttpStatus.CREATED);
	}
	
	//post, put, delete
	//get by id 
	
	@GetMapping("/{id}")
    public ResponseEntity<Students> getStudentById(@PathVariable Long id) {
        return studentsrepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Students> createStudent(@RequestBody Students student) {
        return ResponseEntity.ok(studentsrepo.save(student));
    }

}
