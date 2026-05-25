package com.ssafy.demo_app.domain.user.service;

import com.ssafy.demo_app.api.user.dto.UserResponse;
import com.ssafy.demo_app.api.user.dto.UserRoleUpdateRequest;
import com.ssafy.demo_app.api.user.dto.UserUpdateRequest;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import com.ssafy.demo_app.domain.user.mapper.UserMapper;
import com.ssafy.demo_app.domain.user.model.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
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

        boolean changed = false;
        if (StringUtils.hasText(request.getUserName())) {
            user.setUserName(request.getUserName());
            changed = true;
        }
        if (StringUtils.hasText(request.getDepartment())) {
            user.setDepartment(request.getDepartment());
            changed = true;
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            changed = true;
        } else {
            user.setPassword(null);
        }

        if (changed) {
            userMapper.updateUser(user);
        }

        return UserResponse.from(findUser(userId));
    }

    @Override
    public List<UserResponse> getUsers() {
        return userMapper.findAll()
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
        if (userMapper.updateRole(userId, request.getRole()) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserResponse.from(findUser(userId));
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        if (userMapper.deleteByUserId(userId) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private User findUser(Integer userId) {
        return userMapper.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
