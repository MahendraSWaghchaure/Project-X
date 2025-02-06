package com.app.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Schools {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int schoolId;
	private Students studentId;
	private String SchoolName;
	private Drivers driverId;
	private Parents parentId;
	
	
	public Schools(int schoolId, Students studentId, String schoolName, Drivers driverId, Parents parentId) {
		super();
		this.schoolId = schoolId;
		this.studentId = studentId;
		SchoolName = schoolName;
		this.driverId = driverId;
		this.parentId = parentId;
	}
	public int getSchoolId() {
		return schoolId;
	}
	public void setSchoolId(int schoolId) {
		this.schoolId = schoolId;
	}
	public Students getStudentId() {
		return studentId;
	}
	public void setStudentId(Students studentId) {
		this.studentId = studentId;
	}
	public String getSchoolName() {
		return SchoolName;
	}
	public void setSchoolName(String schoolName) {
		SchoolName = schoolName;
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
