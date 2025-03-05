package com.app.controller;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

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

import com.app.entities.Admin;
import com.app.service.AdminService;

@RestController
@RequestMapping("/api")
public class AdminController {

	@Autowired
	private AdminService adminservice;

	@GetMapping
	public List<Admin> getAllAdmin() // Show the list of all admins
	{
		return adminservice.getAllAdmins();
	}

	@GetMapping("/{id}")
	public ResponseEntity<Admin> getAdminById(@PathVariable Long id) {
		Optional<Admin> admin = adminservice.getAdminById(id);
		return admin.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<Admin> createAdmin(@RequestBody Admin admin) {
		Admin createdAdmin = adminservice.createAdmin(admin);
		return new ResponseEntity<>(createdAdmin, HttpStatus.CREATED);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Admin> updateAdmin(@PathVariable Long id, @RequestBody Admin adminDetails) {
		Admin updatedAdmin = adminservice.updateAdmin(id, adminDetails);
		return updatedAdmin != null ? ResponseEntity.ok(updatedAdmin) : ResponseEntity.notFound().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
		return adminservice.deleteAdmin(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
	
	@GetMapping("/admin-only")
	public ResponseEntity<String> adminOnly(HttpServletRequest request) {
	    if (!"ADMIN".equals(request.getAttribute("role"))) {
	        return ResponseEntity.status(403).body("Forbidden: Admins only");
	    }
	    return ResponseEntity.ok("Welcome, Admin!");
	}


}
