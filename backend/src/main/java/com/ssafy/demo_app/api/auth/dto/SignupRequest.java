package com.ssafy.demo_app.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank(message = "사번은 필수입니다.")
    @Size(max = 50, message = "사번은 50자 이하여야 합니다.")
    private String employeeNo;

    @NotBlank(message = "성명은 필수입니다.")
    @Size(max = 50, message = "성명은 50자 이하여야 합니다.")
    private String userName;

    @NotBlank(message = "부서는 필수입니다.")
    @Size(max = 100, message = "부서는 100자 이하여야 합니다.")
    private String department;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    private String password;

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
