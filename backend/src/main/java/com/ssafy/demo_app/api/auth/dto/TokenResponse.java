package com.ssafy.demo_app.api.auth.dto;

public class TokenResponse {

    private final String tokenType;
    private final String accessToken;
    private final long expiresIn;

    public TokenResponse(String tokenType, String accessToken, long expiresIn) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
