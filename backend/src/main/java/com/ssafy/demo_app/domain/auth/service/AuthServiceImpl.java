package com.ssafy.demo_app.domain.auth.service;

import com.ssafy.demo_app.api.auth.dto.LoginRequest;
import com.ssafy.demo_app.api.auth.dto.LoginResponse;
import com.ssafy.demo_app.api.auth.dto.SignupRequest;
import com.ssafy.demo_app.api.auth.dto.TokenResponse;
import com.ssafy.demo_app.domain.auth.entity.RefreshToken;
import com.ssafy.demo_app.domain.auth.repository.RefreshTokenRepository;
import com.ssafy.demo_app.api.user.dto.UserResponse;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.infrastructure.security.jwt.JwtTokenProvider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmployeeNo(request.getEmployeeNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_NO);
        }

        User user = new User();
        user.setEmployeeNo(request.getEmployeeNo());
        user.setUserName(request.getUserName());
        user.setDepartment(request.getDepartment());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.WORKER);

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional
    public AuthTokenResult login(LoginRequest request) {
        User user = userRepository.findByEmployeeNo(request.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        saveRefreshToken(user, refreshToken);

        return new AuthTokenResult(createLoginResponse(user), refreshToken);
    }

    @Override
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken savedRefreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (savedRefreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            savedRefreshToken.setRevoked(true);
            refreshTokenRepository.save(savedRefreshToken);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return createLoginResponse(savedRefreshToken.getUser());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return;
        }

        refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(refreshToken))
                .ifPresent(savedRefreshToken -> {
                    savedRefreshToken.setRevoked(true);
                    refreshTokenRepository.save(savedRefreshToken);
                });
    }

    private LoginResponse createLoginResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        TokenResponse tokenResponse = new TokenResponse(
                TOKEN_TYPE,
                accessToken,
                jwtTokenProvider.getExpirationMs() / 1000
        );
        return new LoginResponse(tokenResponse, UserResponse.from(user));
    }

    private void saveRefreshToken(User user, String refreshToken) {
        RefreshToken savedRefreshToken = new RefreshToken();
        savedRefreshToken.setUser(user);
        savedRefreshToken.setTokenHash(hashToken(refreshToken));
        savedRefreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationMs() / 1000));
        savedRefreshToken.setRevoked(false);
        refreshTokenRepository.save(savedRefreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }
}
