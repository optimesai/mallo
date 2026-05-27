package com.ssafy.demo_app.api.user.dto;

import com.ssafy.demo_app.domain.user.entity.User;

import java.time.LocalDateTime;

public class UserResponse {

    private final Integer userId;
    private final String employeeNo;
    private final String userName;
    private final String department;
    private final User.Role role;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public UserResponse(
            Integer userId,
            String employeeNo,
            String userName,
            String department,
            User.Role role,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.userId = userId;
        this.employeeNo = employeeNo;
        this.userName = userName;
        this.department = department;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmployeeNo(),
                user.getUserName(),
                user.getDepartment(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public Integer getUserId() {
        return userId;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public String getUserName() {
        return userName;
    }

    public String getDepartment() {
        return department;
    }

    public User.Role getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
