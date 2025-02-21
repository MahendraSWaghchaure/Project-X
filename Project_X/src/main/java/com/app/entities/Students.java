package com.app.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="Students")
public class Students {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	//One To One
	private int studentId;
	
	@Column(nullable =false)
	private String studentName;
	
	@Column(nullable =false)
	//One To Many
	private String studentSchool;
	
	@Column(nullable =false)
	//One To One
	private String studentClass;
	
	@Column(nullable =false)
	private Parents parentId;
	
	@Column(nullable =false)
	private Drivers driverId;
	
	@Column(nullable =false)
	private String studentRefrenceNo;		//Unique field - No duplicates
	
	
	public Students(int studentId, String studentName, String studentSchool, String studentClass, Parents parentId,
			Drivers driverId, String studentRefrenceNo) {
		super();
		this.studentId = studentId;
		this.studentName = studentName;
		this.studentSchool = studentSchool;
		this.studentClass = studentClass;
		this.parentId = parentId;
		this.driverId = driverId;
		this.studentRefrenceNo = studentRefrenceNo;
	}
	
	public String getStudentRefrenceNo() {
		return studentRefrenceNo;
	}

	public void setStudentRefrenceNo(String studentRefrenceNo) {
		this.studentRefrenceNo = studentRefrenceNo;
	}

	public int getStudentId() {
		return studentId;
	}
	public void setStudentId(int studentId) {
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
