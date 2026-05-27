package com.ssafy.demo_app.api.user.dto;

import com.ssafy.demo_app.domain.user.model.User;
import jakarta.validation.constraints.NotNull;

public class UserRoleUpdateRequest {

    @NotNull(message = "권한은 필수입니다.")
    private User.Role role;

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }
}
