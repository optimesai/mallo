package com.ssafy.demo_app.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "사번은 필수입니다.")
    @Size(max = 50, message = "사번은 50자 이하여야 합니다.")
    private String employeeNo;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
