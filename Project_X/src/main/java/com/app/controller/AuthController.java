package com.app.controller;

import com.app.dto.SignupRequest;
import com.app.entities.Admin;
import com.app.entities.Drivers;
import com.app.entities.Parents;
import com.app.repo.AdminRepository;
import com.app.repo.DriversRepository;
import com.app.repo.ParentsRepository;
import com.app.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AdminRepository adminRepo;

    @Autowired
    private DriversRepository driverRepo;

    @Autowired
    private ParentsRepository parentRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody SignupRequest request) {
        Optional<Admin> admin = adminRepo.findByEmail(request.getUsername());
        Optional<Drivers> driver = driverRepo.findByUserName(request.getUsername());
        Optional<Parents> parent = parentRepo.findByPusername(request.getUsername());

        if (admin.isPresent() && passwordEncoder.matches(request.getPassword(), admin.get().getPassword())) {
            String token = jwtUtil.generateToken(admin.get().getEmail(), "ADMIN", admin.get().getId());
            return ResponseEntity.ok(token);
        } else if (driver.isPresent() && passwordEncoder.matches(request.getPassword(), driver.get().getDpassword())) {
            String token = jwtUtil.generateToken(driver.get().getDusername(), "DRIVER", driver.get().getDriverId());
            return ResponseEntity.ok(token);
        } else if (parent.isPresent() && passwordEncoder.matches(request.getPassword(), parent.get().getPpassword())) {
            String token = jwtUtil.generateToken(parent.get().getPusername(), "PARENT", parent.get().getParentId());
            return ResponseEntity.ok(token);
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
