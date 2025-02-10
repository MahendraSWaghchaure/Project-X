package com.app.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.entities.Drivers;
import com.app.repo.DriversRepository;

@Service
public class DriversService {

	@Autowired
	private DriversRepository driverrepo;
	
	public List<Drivers> getAllDrivers()
	{
		return driverrepo.findAll();
	}
	
}
