package com.ssafy.demo_app.api.auth.dto;

import com.ssafy.demo_app.api.user.dto.UserResponse;

public class LoginResponse {

    private final TokenResponse token;
    private final UserResponse user;

    public LoginResponse(TokenResponse token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public TokenResponse getToken() {
        return token;
    }

    public UserResponse getUser() {
        return user;
    }
}
