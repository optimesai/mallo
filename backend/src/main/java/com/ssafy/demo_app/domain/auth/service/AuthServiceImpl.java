package com.ssafy.demo_app.domain.auth.service;

import com.ssafy.demo_app.api.auth.dto.LoginRequest;
import com.ssafy.demo_app.api.auth.dto.LoginResponse;
import com.ssafy.demo_app.api.auth.dto.SignupRequest;
import com.ssafy.demo_app.api.auth.dto.TokenResponse;
import com.ssafy.demo_app.api.user.dto.UserResponse;
import com.ssafy.demo_app.domain.user.mapper.UserMapper;
import com.ssafy.demo_app.domain.user.model.User;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.infrastructure.security.jwt.JwtTokenProvider;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userMapper.existsByEmployeeNo(request.getEmployeeNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_NO);
        }

        User user = new User();
        user.setEmployeeNo(request.getEmployeeNo());
        user.setUserName(request.getUserName());
        user.setDepartment(request.getDepartment());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.WORKER);

        userMapper.insertUser(user);

        User savedUser = userMapper.findByEmployeeNo(user.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(savedUser);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByEmployeeNo(request.getEmployeeNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user);
        TokenResponse tokenResponse = new TokenResponse(
                TOKEN_TYPE,
                accessToken,
                jwtTokenProvider.getExpirationMs() / 1000
        );
        return new LoginResponse(tokenResponse, UserResponse.from(user));
    }
}
