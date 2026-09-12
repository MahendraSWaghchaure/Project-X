package com.projectx.people.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** routeId/insurancePolicyId are cross-module id references (module-school / module-insurance), no FK. */
@Entity
@Table(name = "children")
public class Child extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    /** The one guardian route auto-allocation and fee-invoice billing key off - see GuardianAccessService for the full (unlimited) guardian list, which is this field plus any child_guardians rows. */
    @Column(name = "primary_parent_id", nullable = false)
    private Long primaryParentId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "class_section")
    private String classSection;

    @Column(name = "route_id")
    private Long routeId;

    /** Which RouteStop (module-school) is "their" stop - approaching/arriving there is what triggers this child's parent alerts. */
    @Column(name = "route_stop_id")
    private Long routeStopId;

    @Column(name = "insurance_policy_id")
    private Long insurancePolicyId;

    @Column(name = "blood_type", length = 8)
    private String bloodType;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 32)
    private String emergencyContactPhone;

    @Column(name = "allergies", length = 500)
    private String allergies;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChildStatus status = ChildStatus.ACTIVE;

    protected Child() {
    }

    public Child(Long schoolId, Long primaryParentId, String firstName, String lastName,
                 LocalDate dateOfBirth, String classSection) {
        this.schoolId = schoolId;
        this.primaryParentId = primaryParentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.classSection = classSection;
    }

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public Long getPrimaryParentId() {
        return primaryParentId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getClassSection() {
        return classSection;
    }

    public void updateDetails(String firstName, String lastName, LocalDate dateOfBirth, String classSection) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.classSection = classSection;
    }

    public String getBloodType() {
        return bloodType;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public String getAllergies() {
        return allergies;
    }

    public void updateMedicalSafetyInfo(String bloodType, String emergencyContactName, String emergencyContactPhone, String allergies) {
        this.bloodType = bloodType;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.allergies = allergies;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Long getRouteStopId() {
        return routeStopId;
    }

    public void setRouteStopId(Long routeStopId) {
        this.routeStopId = routeStopId;
    }

    public Long getInsurancePolicyId() {
        return insurancePolicyId;
    }

    public void setInsurancePolicyId(Long insurancePolicyId) {
        this.insurancePolicyId = insurancePolicyId;
    }

    public ChildStatus getStatus() {
        return status;
    }

    public void setStatus(ChildStatus status) {
        this.status = status;
    }
}
