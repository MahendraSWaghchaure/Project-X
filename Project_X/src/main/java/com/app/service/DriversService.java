package com.app.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.entities.Drivers;
import com.app.entities.Students;
import com.app.repo.DriversRepository;

@Service
public class DriversService {

    @Autowired
    private DriversRepository driverrepo;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public List<Drivers> getAllDrivers() {
        return driverrepo.findAll();
    }

    public Optional<Drivers> getDriverById(Long id) {
        return driverrepo.findById(id);
    }

    public Drivers createDriver(Drivers driver) {
        driver.setDpassword(passwordEncoder.encode(driver.getDpassword())); // Encrypt password before saving
        return driverrepo.save(driver);
    }

    public Drivers updateDriver(Long id, Drivers driverDetail) {
        return driverrepo.findById(id).map(driver -> {
            driver.setdFirstName_(driverDetail.getdFirstName_());
            driver.setdLastName_(driverDetail.getdLastName_());
            driver.setVehicleNo(driverDetail.getVehicleNo());
            driver.setMobileNo(driverDetail.getMobileNo());
            driver.setDusername(driverDetail.getDusername());
            driver.setStudentsId(driverDetail.getStudentsId());
            return driverrepo.save(driver);
        }).orElse(null);
    }

    public boolean deleteDriver(Long id) {
        if (driverrepo.existsById(id)) {
            driverrepo.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<Drivers> findByVehicleNo(String vehicleNo) {
        return driverrepo.findByVehicleNo(vehicleNo);
    }

    public String updateDriverPass(String username, String newPassword) {
        Optional<Drivers> driverPass = driverrepo.findByUserName(username);
        if (driverPass.isPresent()) {
            Drivers driver = driverPass.get();
            driver.setDpassword(passwordEncoder.encode(newPassword));
            driverrepo.save(driver);
            return "Password successfully updated.";
        } else {
            return "Driver not found with username: " + username;
        }
    }

    public List<Drivers> getDriversByName(String name) {
        return driverrepo.findBydFirstName_(name); 
    }

    public List<Drivers> getDriversBySchool(String school) {
        return driverrepo.findBySchool(school);
    }

    public List<String> getStudentNamesByDriverId(Long id) {
        return (List<String>) driverrepo.findById(id)
                .map(driver -> {
                    Students student = driver.getStudentsId();
                    return student != null ? List.of(student.getStudentName()) : Collections.emptyList();
                })
                .orElse(Collections.emptyList());
    }

}
