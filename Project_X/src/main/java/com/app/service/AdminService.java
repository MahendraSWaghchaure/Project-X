package com.app.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.entities.Admin;
import com.app.repo.AdminRepository;

@Service
public class AdminService {
	
	@Autowired
	private AdminRepository adminrepo; //connect admin repo with service
	
	public List<Admin> getAllAdmins()
	{
		return adminrepo.findAll();
	}
	
	public Optional<Admin> getAdminById(Long id)
	{
		return adminrepo.findById(id);
	}
	
	public Admin createAdmin(Admin admin)
	{
		return adminrepo.save(admin);
	}
	
	public Admin updateAdmin(Long id, Admin adminDetails)
	{
		if(adminrepo.existsById(id))
		{
			adminDetails.setId(id);
			return adminrepo.save(adminDetails);
		}
		return null;
	}
	
	public boolean deleteAdmin(Long id)
	{
		if(adminrepo.existsById(id))
		{
			adminrepo.deleteById(id);
			return true;
		}
		return false;
	}
}
