package com.app.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.entities.Schools;
import com.app.repo.SchoolsRepository;

@Service
public class SchoolsService {
	
	@Autowired
	private static SchoolsRepository schoolrepo;
	
	//Create the Schools Profile
	public Schools createProfile(Schools profile) {
		return schoolrepo.save(profile);
	}
	
	//update, delete, select <list>
	public static List<Schools> GetAllSchools(){
		return schoolrepo.findAll();
	}
	
	//School needs to have access to student profile to change any incorrect data related to his class
	
	
	public void deleteSchoolProfile(Long id) {
		schoolrepo.deleteById(id);
	}
}
