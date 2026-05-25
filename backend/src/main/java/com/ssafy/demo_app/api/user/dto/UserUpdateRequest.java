package com.ssafy.demo_app.api.user.dto;

import jakarta.validation.constraints.Size;

public class UserUpdateRequest {

    @Size(max = 50, message = "성명은 50자 이하여야 합니다.")
    private String userName;

    @Size(max = 100, message = "부서는 100자 이하여야 합니다.")
    private String department;

    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    private String password;

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
