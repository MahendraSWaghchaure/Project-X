package com.app.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.entities.Students;
import com.app.repo.DriversRepository;
import com.app.repo.StudentsRepository;

@Service
public class StudentsService {

    @Autowired
    private static StudentsRepository studentsrepo;

    @Autowired
    private DriversRepository driverrepo;

    // Fetch all students
    public static List<Students> getAllStudents() {
        return studentsrepo.findAll();
    }

    // Get student names by driver ID
//    public List<String> getStudentNamesByDriverId(Long id) {
//        return driverrepo.findById(id)
//                .map(driver -> {
//                    Students student = driver.getStudentsId();
//                    return (student != null) ? List.of(student.getStudentName()) : Collections.emptyList();
//                })
//                .orElse(Collections.emptyList());
//    }
}
