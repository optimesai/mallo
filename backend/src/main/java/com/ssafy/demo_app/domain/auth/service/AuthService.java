package com.ssafy.demo_app.domain.auth.service;

import com.ssafy.demo_app.api.auth.dto.LoginRequest;
import com.ssafy.demo_app.api.auth.dto.LoginResponse;
import com.ssafy.demo_app.api.auth.dto.SignupRequest;
import com.ssafy.demo_app.api.user.dto.UserResponse;

public interface AuthService {

    UserResponse signup(SignupRequest request);

    LoginResponse login(LoginRequest request);
}
