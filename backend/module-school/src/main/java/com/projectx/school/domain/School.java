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
import jakarta.persistence.Version;
import java.time.LocalTime;
import org.locationtech.jts.geom.Point;

/** The tenant root. A vehicle "approaching school" alert fires when it enters {@code geofenceRadiusMeters} of {@code location}. */
@Entity
@Table(name = "schools")
public class School extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Guards concurrent admin edits (two tabs, or platform admin + school admin editing at once) from silently losing one. */
    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    private String state;
    private String pincode;

    /**
     * Optional structured link into module-geo's reference data (country/state/region/district),
     * additive alongside the free-text city/state/pincode above rather than replacing them - a
     * school onboarded before this existed simply has this null, and city/state keep working
     * exactly as before. Cross-module id reference, no FK constraint - same convention as routeId/
     * insurancePolicyId elsewhere in this codebase, since module-school doesn't depend on
     * module-geo (only app composes both).
     */
    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "geofence_radius_meters", nullable = false)
    private int geofenceRadiusMeters = 300;

    /** When the school day ends - module-tracking's delay monitor alerts parents/admins if a drop-off trip is still IN_PROGRESS this long past dismissal. */
    @Column(name = "dismissal_time")
    private LocalTime dismissalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SchoolStatus status = SchoolStatus.ACTIVE;

    /** Why the school is SUSPENDED (required when suspending; cleared on re-activation). */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "status_changed_at")
    private java.time.Instant statusChangedAt;

    protected School() {
    }

    public School(String name, String address, String city, String state, String pincode,
                  String contactPhone, String contactEmail, Point location) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void updateDetails(String name, String address, String city, String state, String pincode,
                               String contactPhone, String contactEmail) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public int getGeofenceRadiusMeters() {
        return geofenceRadiusMeters;
    }

    public void setGeofenceRadiusMeters(int geofenceRadiusMeters) {
        this.geofenceRadiusMeters = geofenceRadiusMeters;
    }

    public LocalTime getDismissalTime() {
        return dismissalTime;
    }

    public void setDismissalTime(LocalTime dismissalTime) {
        this.dismissalTime = dismissalTime;
    }

    public SchoolStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public java.time.Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    /** The only way to change status: records when, and why (reason is kept only while SUSPENDED). */
    public void changeStatus(SchoolStatus status, String reason) {
        this.status = status;
        this.statusReason = status == SchoolStatus.SUSPENDED ? reason : null;
        this.statusChangedAt = java.time.Instant.now();
    }
}
