package vn.truongngo.android.no3.model;

import java.io.Serializable;

public class Employee implements Serializable {
    private String employeeCode;
    private String fullName;
    private int birthYear;
    private String gender;
    private String hometown;
    private String educationLevel;
    private String avatar; // Lưu chuỗi URI hoặc tên drawable
    private double salary;
    private String department;

    public Employee() {
    }

    public Employee(String employeeCode, String fullName, int birthYear, String gender, 
                    String hometown, String educationLevel, String avatar, double salary, String department) {
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.birthYear = birthYear;
        this.gender = gender;
        this.hometown = hometown;
        this.educationLevel = educationLevel;
        this.avatar = avatar;
        this.salary = salary;
        this.department = department;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
