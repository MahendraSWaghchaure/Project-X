package com.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.dto.SignupRequest;
import com.app.repo.ParentsRepository;

@RestController
@RequestMapping("/api")
public class AdminController {
	
	@Autowired
	private ParentsRepository parentrepo;
	
	@PostMapping("/signup")
	public ResponseEntity<?> regiterUser(@RequestBody SignupRequest signupRequest){
		if() {
			
		}
	}

}
