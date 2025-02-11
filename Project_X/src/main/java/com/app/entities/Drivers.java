package com.app.entities;

import java.time.LocalDateTime;

import javax.persistence.*;
import javax.validation.constraints.Pattern;



@Entity
@Table(name="Drivers")
public class Drivers {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long driverId;
	
	@Column(nullable = false)
	private String dFirstName_;
	
	@Column(nullable = false)
	private String dLastName_;
	
	@Column(nullable = false, unique = true)
	private String vehicleNo;
	
	@Column(nullable = false, unique = true)
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits")
    private String mobileNo;
	
	@Column(nullable = false, unique = true)
    private String dusername;

    @Column(nullable = false)
    private String dpassword;

    @Column(nullable = false)
    private boolean isLoggedIn;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column(nullable = false)
    private LocalDateTime updatedOn;
    
    @OneToOne
    @JoinColumn(name = "students", referencedColumnName = "studentsId")
    private Students studentsId;
    
    
    // Default Constructor
    public Drivers() {
    	
    }

    //Parameterized Constructors
	
	public Drivers(Long driverId, String dFirstName_, String dLastName_, String vehicleNo,
			@Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits") String mobileNo,
			String dusername, String dpassword, boolean isLoggedIn, LocalDateTime createdOn, LocalDateTime updatedOn,
			Students studentsId) {
		super();
		this.driverId = driverId;
		this.dFirstName_ = dFirstName_;
		this.dLastName_ = dLastName_;
		this.vehicleNo = vehicleNo;
		this.mobileNo = mobileNo;
		this.dusername = dusername;
		this.dpassword = dpassword;
		this.isLoggedIn = isLoggedIn;
		this.createdOn = createdOn;
		this.updatedOn = updatedOn;
		this.studentsId = studentsId;
	}

	public Long getDriverId() {
		return driverId;
	}

	public void setDriverId(Long driverId) {
		this.driverId = driverId;
	}

	public String getdFirstName_() {
		return dFirstName_;
	}

	public void setdFirstName_(String dFirstName_) {
		this.dFirstName_ = dFirstName_;
	}

	public String getdLastName_() {
		return dLastName_;
	}

	public void setdLastName_(String dLastName_) {
		this.dLastName_ = dLastName_;
	}

	public String getVehicleNo() {
		return vehicleNo;
	}

	public void setVehicleNo(String vehicleNo) {
		this.vehicleNo = vehicleNo;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}

	public String getDusername() {
		return dusername;
	}

	public void setDusername(String dusername) {
		this.dusername = dusername;
	}

	public String getDpassword() {
		return dpassword;
	}

	public void setDpassword(String dpassword) {
		this.dpassword = dpassword;
	}

	public boolean isLoggedIn() {
		return isLoggedIn;
	}

	public void setLoggedIn(boolean isLoggedIn) {
		this.isLoggedIn = isLoggedIn;
	}

	public LocalDateTime getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(LocalDateTime createdOn) {
		this.createdOn = createdOn;
	}

	public LocalDateTime getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(LocalDateTime updatedOn) {
		this.updatedOn = updatedOn;
	}

	public Students getStudentsId() {
		return studentsId;
	}

	public void setStudentsId(Students studentsId) {
		this.studentsId = studentsId;
	}

 
    
	
}
