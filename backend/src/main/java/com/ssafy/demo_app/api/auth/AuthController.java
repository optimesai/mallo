package com.ssafy.demo_app.api.auth;

import com.ssafy.demo_app.api.auth.dto.LoginRequest;
import com.ssafy.demo_app.api.auth.dto.LoginResponse;
import com.ssafy.demo_app.api.auth.dto.SignupRequest;
import com.ssafy.demo_app.api.user.dto.UserResponse;
import com.ssafy.demo_app.domain.auth.service.AuthTokenResult;
import com.ssafy.demo_app.domain.auth.service.AuthService;
import com.ssafy.demo_app.global.response.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final long refreshTokenExpirationMs;
    private final boolean refreshTokenCookieSecure;

    public AuthController(
            AuthService authService,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
            @Value("${jwt.refresh-token-cookie-secure:false}") boolean refreshTokenCookieSecure
    ) {
        this.authService = authService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshTokenCookieSecure = refreshTokenCookieSecure;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse response = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @GetMapping("/employee-no/exists")
    public ResponseEntity<ApiResponse<Boolean>> existsByEmployeeNo(@RequestParam String employeeNo) {
        return ResponseEntity.ok(ApiResponse.success(authService.existsByEmployeeNo(employeeNo)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(result.getRefreshToken()).toString())
                .body(ApiResponse.success("로그인이 완료되었습니다.", result.getLoginResponse()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        LoginResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie().toString())
                .body(ApiResponse.success("로그아웃이 완료되었습니다."));
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .build();
    }

    private ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
