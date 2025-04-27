package com.app.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Students {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long studentId;
	private String studentName;
	private String studentSchool;
	private String studentClass;
	private Parents parentId;
	private Drivers driverId;
	
	
	public Students(Long studentId, String studentName, String studentSchool, String studentClass, Parents parentId,Drivers driverId) {
		super();
		this.studentId = studentId;
		this.studentName = studentName;
		this.studentSchool = studentSchool;
		this.studentClass = studentClass;
		this.parentId = parentId;
		this.driverId = driverId;
	}
	public Long getStudentId() {
		return studentId;
	}
	public void setStudentId(Long studentId) {
		this.studentId = studentId;
	}
	public String getStudentName() {
		return studentName;
	}
	public void setStudentName(String studentName) {
		this.studentName = studentName;
	}
	public String getStudentSchool() {
		return studentSchool;
	}
	public void setStudentSchool(String studentSchool) {
		this.studentSchool = studentSchool;
	}
	public String getStudentClass() {
		return studentClass;
	}
	public void setStudentClass(String studentClass) {
		this.studentClass = studentClass;
	}
	public Parents getParentId() {
		return parentId;
	}
	public void setParentId(Parents parentId) {
		this.parentId = parentId;
	}
	public Drivers getDriverId() {
		return driverId;
	}
	public void setDriverId(Drivers driverId) {
		this.driverId = driverId;
	}
	
}
