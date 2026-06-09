package com.ssafy.demo_app.domain.auth.service;

import com.ssafy.demo_app.api.auth.dto.LoginResponse;

public class AuthTokenResult {

    private final LoginResponse loginResponse;
    private final String refreshToken;

    public AuthTokenResult(LoginResponse loginResponse, String refreshToken) {
        this.loginResponse = loginResponse;
        this.refreshToken = refreshToken;
    }

    public LoginResponse getLoginResponse() {
        return loginResponse;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
