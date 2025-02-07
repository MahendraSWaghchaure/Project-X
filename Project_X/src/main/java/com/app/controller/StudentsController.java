package com.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.app.service.StudentsService;

@RestController

public class StudentsController {
	
	private static final Logger logger= LoggerFactory.getLogger(StudentsController.class);
	
	@Autowired
	private StudentsService studentProfileService;
}
