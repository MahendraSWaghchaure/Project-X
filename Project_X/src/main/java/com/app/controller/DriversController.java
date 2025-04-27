package com.app.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.app.entities.Drivers;
import com.app.service.DriversService;

@RestController
@RequestMapping("/drivers")
public class DriversController {

    @Autowired
    private DriversService driverService;

    @GetMapping
    public List<Drivers> getAllDrivers() {
        return driverService.getAllDrivers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Drivers> getDriverById(@PathVariable Long id) {
        return driverService.getDriverById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Drivers> createDriver(@RequestBody Drivers driver) {
        return ResponseEntity.ok(driverService.createDriver(driver));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Drivers> updateDriver(@PathVariable Long id, @RequestBody Drivers driverDetail) {
        Drivers updatedDriver = driverService.updateDriver(id, driverDetail);
        return updatedDriver != null ? ResponseEntity.ok(updatedDriver) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDriver(@PathVariable Long id) {
        return driverService.deleteDriver(id)
                ? ResponseEntity.ok("Driver deleted successfully.")
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/vehicle/{vehicleNo}")
    public ResponseEntity<Drivers> getDriverByVehicleNo(@PathVariable String vehicleNo) {
        return driverService.findByVehicleNo(vehicleNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/update-password/{username}")
    public ResponseEntity<String> updatePassword(@PathVariable String username, @RequestParam String newPassword) {
        return ResponseEntity.ok(driverService.updateDriverPass(username, newPassword));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<List<Drivers>> getDriversByName(@PathVariable String name) {
        List<Drivers> drivers = driverService.getDriversByName(name);
        return drivers.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(drivers);
    }

    @GetMapping("/school/{school}")
    public ResponseEntity<List<Drivers>> getDriversBySchool(@PathVariable String school) {
        List<Drivers> drivers = driverService.getDriversBySchool(school);
        return drivers.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(drivers);
    }

//    @GetMapping("/{id}/students")
//    public ResponseEntity<List<String>> getStudentsByDriver(@PathVariable Long id) {
//        List<String> students = driverService.getStudentsByDriver(id);
//        return students == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(students);
//    }
    
    
}
