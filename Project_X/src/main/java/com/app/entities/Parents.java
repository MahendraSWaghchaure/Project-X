package com.app.entities;

import javax.persistence.*;
import javax.validation.constraints.Pattern;


@Entity
@Table(name="Parents")
public class Parents {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long parentId;
	
	@Column(nullable =false)
	private String pFirstName;
	
	@Column(nullable = false)
	private String pLastName;
	
	@Column(nullable = false)
	private String email;
	
	@Column(nullable = false)
	private String address;
	
	@Column(nullable = false, unique = true)
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits")
    private String mobileNo;
	
	@Column(nullable = false, unique = true)
    private String pusername;

    @Column(nullable = false)
    private String ppassword;

    @OneToOne
    @JoinColumn(name = "students", referencedColumnName = "studentsId")
    private Students studentsId;

	public Parents() {
		super();
	}

	public Parents(Long parentId, String pFirstName, String pLastName, String email, String address,
			@Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits") String mobileNo,
			String pusername, String ppassword, Students studentsId) {
		super();
		this.parentId = parentId;
		this.pFirstName = pFirstName;
		this.pLastName = pLastName;
		this.email = email;
		this.address = address;
		this.mobileNo = mobileNo;
		this.pusername = pusername;
		this.ppassword = ppassword;
		this.studentsId = studentsId;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public String getpFirstName() {
		return pFirstName;
	}

	public void setpFirstName(String pFirstName) {
		this.pFirstName = pFirstName;
	}

	public String getpLastName() {
		return pLastName;
	}

	public void setpLastName(String pLastName) {
		this.pLastName = pLastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}

	public String getPusername() {
		return pusername;
	}

	public void setPusername(String pusername) {
		this.pusername = pusername;
	}

	public String getPpassword() {
		return ppassword;
	}

	public void setPpassword(String ppassword) {
		this.ppassword = ppassword;
	}

	public Students getStudentsId() {
		return studentsId;
	}

	public void setStudentsId(Students studentsId) {
		this.studentsId = studentsId;
	}
    
    
    
    
    
    
}
