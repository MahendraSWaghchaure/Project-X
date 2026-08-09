package com.projectx.school.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * insurancePolicyId is a cross-module reference by id only (module-insurance owns that table) -
 * deliberately no FK constraint, so modules stay independently extractable/splittable later.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VehicleType type;

    @Column(name = "seat_capacity", nullable = false)
    private int seatCapacity;

    /** Traccar's device "uniqueId" - correlates incoming position reports to this vehicle. */
    @Column(name = "gps_device_id", unique = true)
    private String gpsDeviceId;

    @Column(name = "insurance_policy_id")
    private Long insurancePolicyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VehicleStatus status = VehicleStatus.ACTIVE;

    protected Vehicle() {
    }

    public Vehicle(Long schoolId, String registrationNumber, VehicleType type, int seatCapacity, String gpsDeviceId) {
        this.schoolId = schoolId;
        this.registrationNumber = registrationNumber;
        this.type = type;
        this.seatCapacity = seatCapacity;
        this.gpsDeviceId = gpsDeviceId;
    }

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public VehicleType getType() {
        return type;
    }

    public int getSeatCapacity() {
        return seatCapacity;
    }

    public String getGpsDeviceId() {
        return gpsDeviceId;
    }

    public void setGpsDeviceId(String gpsDeviceId) {
        this.gpsDeviceId = gpsDeviceId;
    }

    public Long getInsurancePolicyId() {
        return insurancePolicyId;
    }

    public void setInsurancePolicyId(Long insurancePolicyId) {
        this.insurancePolicyId = insurancePolicyId;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }
}
