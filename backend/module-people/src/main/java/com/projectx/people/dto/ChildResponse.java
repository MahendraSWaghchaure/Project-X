package com.projectx.people.dto;

import com.projectx.people.domain.Child;
import com.projectx.people.domain.ChildStatus;
import java.time.LocalDate;

public record ChildResponse(
        Long id, Long schoolId, Long primaryParentId, String firstName, String lastName,
        LocalDate dateOfBirth, String classSection, Long routeId, Long routeStopId, Long insurancePolicyId,
        String bloodType, String emergencyContactName, String emergencyContactPhone, String allergies,
        ChildStatus status) {

    public static ChildResponse from(Child child) {
        return new ChildResponse(child.getId(), child.getSchoolId(), child.getPrimaryParentId(),
                child.getFirstName(), child.getLastName(), child.getDateOfBirth(),
                child.getClassSection(), child.getRouteId(), child.getRouteStopId(), child.getInsurancePolicyId(),
                child.getBloodType(), child.getEmergencyContactName(), child.getEmergencyContactPhone(),
                child.getAllergies(), child.getStatus());
    }
}
