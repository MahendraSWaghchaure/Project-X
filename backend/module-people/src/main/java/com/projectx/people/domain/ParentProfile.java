package com.projectx.people.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

/**
 * invitedBySchoolId is the school whose admin sent the invite - the authoritative default school
 * for any child this parent registers, so a parent can never claim a school they weren't invited by.
 */
@Entity
@Table(name = "parent_profiles")
public class ParentProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "invited_by_school_id", nullable = false)
    private Long invitedBySchoolId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "home_location", columnDefinition = "geometry(Point,4326)")
    private Point homeLocation;

    protected ParentProfile() {
    }

    public ParentProfile(Long userId, Long invitedBySchoolId, String firstName, String lastName) {
        this.userId = userId;
        this.invitedBySchoolId = invitedBySchoolId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getInvitedBySchoolId() {
        return invitedBySchoolId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public Point getHomeLocation() {
        return homeLocation;
    }

    public void completeProfile(String firstName, String lastName, String phone, String address, Point homeLocation) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.homeLocation = homeLocation;
    }
}
