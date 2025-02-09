package com.app.entities;

import javax.persistence.*;

@Entity
public class Schools {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long schoolId;

	    @OneToOne
	    @JoinColumn(name = "student_id", nullable = false)
	    private Students studentId;

	    @Column(nullable = false)
	    private String schoolName;

	    @OneToOne
	    @JoinColumn(name = "driver_id", nullable = false)
	    private Drivers driverId;

	    @OneToOne
	    @JoinColumn(name = "parent_id", nullable = false)
	    private Parents parentId;
	
	
	public Schools(Long schoolId, Students studentId, String schoolName, Drivers driverId, Parents parentId) {
		super();
		this.schoolId = schoolId;
		this.studentId = studentId;
		this.schoolName = schoolName;
		this.driverId = driverId;
		this.parentId = parentId;
	}
	public Long getSchoolId() {
		return schoolId;
	}
	public void setSchoolId(Long schoolId) {
		this.schoolId = schoolId;
	}
	public Students getStudentId() {
		return studentId;
	}
	public void setStudentId(Students studentId) {
		this.studentId = studentId;
	}
	public String getSchoolName() {
		return schoolName;
	}
	public void setSchoolName(String schoolName) {
		schoolName = schoolName;
	}
	public Drivers getDriverId() {
		return driverId;
	}
	public void setDriverId(Drivers driverId) {
		this.driverId = driverId;
	}
	public Parents getParentId() {
		return parentId;
	}
	public void setParentId(Parents parentId) {
		this.parentId = parentId;
	}
}
