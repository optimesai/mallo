package com.ssafy.demo_app.domain.user.service;

import com.ssafy.demo_app.api.user.dto.UserResponse;
import com.ssafy.demo_app.api.user.dto.UserRoleUpdateRequest;
import com.ssafy.demo_app.api.user.dto.UserUpdateRequest;
import com.ssafy.demo_app.domain.auth.entity.RefreshToken;
import com.ssafy.demo_app.domain.auth.repository.RefreshTokenRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.domain.user.entity.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse getMyInfo(Integer userId) {
        return UserResponse.from(findUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateMyInfo(Integer userId, UserUpdateRequest request) {
        User user = findUser(userId);

        if (StringUtils.hasText(request.getUserName())) {
            user.setUserName(request.getUserName());
        }
        if (StringUtils.hasText(request.getDepartment())) {
            user.setDepartment(request.getDepartment());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            revokeRefreshTokens(user);
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Override
    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    public UserResponse getUser(Integer userId) {
        return UserResponse.from(findUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateRole(Integer userId, UserRoleUpdateRequest request) {
        User user = findUser(userId);
        user.setRole(request.getRole());
        revokeRefreshTokens(user);
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        User user = findUser(userId);
        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void revokeRefreshTokens(User user) {
        List<RefreshToken> refreshTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        refreshTokens.forEach(refreshToken -> refreshToken.setRevoked(true));
        refreshTokenRepository.saveAll(refreshTokens);
    }
}
