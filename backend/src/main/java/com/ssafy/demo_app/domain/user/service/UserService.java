package com.ssafy.demo_app.domain.user.service;

import com.ssafy.demo_app.api.user.dto.UserResponse;
import com.ssafy.demo_app.api.user.dto.UserRoleUpdateRequest;
import com.ssafy.demo_app.api.user.dto.UserUpdateRequest;

import java.util.List;

public interface UserService {

    UserResponse getMyInfo(Integer userId);

    UserResponse updateMyInfo(Integer userId, UserUpdateRequest request);

    List<UserResponse> getUsers();

    UserResponse getUser(Integer userId);

    UserResponse updateRole(Integer adminUserId, Integer userId, UserRoleUpdateRequest request);

    void deleteUser(Integer adminUserId, Integer userId);
}
