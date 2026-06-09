package com.ssafy.demo_app.domain.user.service;

import com.ssafy.demo_app.api.user.dto.UserRoleUpdateRequest;
import com.ssafy.demo_app.domain.auth.repository.RefreshTokenRepository;
import com.ssafy.demo_app.domain.user.entity.User;
import com.ssafy.demo_app.domain.user.repository.UserRepository;
import com.ssafy.demo_app.global.exception.BusinessException;
import com.ssafy.demo_app.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User admin;
    private User anotherAdmin;
    private User worker;

    @BeforeEach
    void setUp() {
        admin = createUser(1, "EMP-ADMIN-01", User.Role.ADMIN);
        anotherAdmin = createUser(2, "EMP-ADMIN-02", User.Role.ADMIN);
        worker = createUser(3, "EMP-WORKER-01", User.Role.WORKER);
    }

    @Test
    @DisplayName("권한 변경 실패 - 본인 권한 변경")
    void updateRole_selfRoleChange() {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRole(User.Role.WORKER);

        given(userRepository.findById(1)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.updateRole(1, 1, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_SELF_ROLE_CHANGE_FORBIDDEN);

        verify(userRepository, never()).save(admin);
    }

    @Test
    @DisplayName("권한 변경 실패 - 마지막 관리자 강등")
    void updateRole_lastAdminDemotion() {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRole(User.Role.MANAGER);

        given(userRepository.findById(2)).willReturn(Optional.of(anotherAdmin));
        given(userRepository.countByRole(User.Role.ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> userService.updateRole(1, 2, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LAST_ADMIN_CHANGE_FORBIDDEN);

        verify(userRepository, never()).save(anotherAdmin);
    }

    @Test
    @DisplayName("권한 변경 성공 - 작업자 권한 승격")
    void updateRole_workerPromotion() {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest();
        request.setRole(User.Role.MANAGER);

        given(userRepository.findById(3)).willReturn(Optional.of(worker));
        given(refreshTokenRepository.findByUserAndRevokedFalse(worker)).willReturn(List.of());

        userService.updateRole(1, 3, request);

        verify(refreshTokenRepository).saveAll(List.of());
        verify(userRepository).save(worker);
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 본인 계정 삭제")
    void deleteUser_selfDelete() {
        given(userRepository.findById(1)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.deleteUser(1, 1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_SELF_DELETE_FORBIDDEN);

        verify(userRepository, never()).delete(admin);
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 마지막 관리자 삭제")
    void deleteUser_lastAdminDelete() {
        given(userRepository.findById(2)).willReturn(Optional.of(anotherAdmin));
        given(userRepository.countByRole(User.Role.ADMIN)).willReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(1, 2))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LAST_ADMIN_CHANGE_FORBIDDEN);

        verify(userRepository, never()).delete(anotherAdmin);
    }

    @Test
    @DisplayName("사용자 삭제 성공 - 작업자 삭제")
    void deleteUser_workerDelete() {
        given(userRepository.findById(3)).willReturn(Optional.of(worker));

        userService.deleteUser(1, 3);

        verify(refreshTokenRepository).deleteByUser(worker);
        verify(userRepository).delete(worker);
    }

    private User createUser(Integer userId, String employeeNo, User.Role role) {
        User user = new User();
        user.setUserId(userId);
        user.setEmployeeNo(employeeNo);
        user.setUserName("테스트 사용자");
        user.setDepartment("테스트 부서");
        user.setPassword("encoded-password");
        user.setRole(role);
        return user;
    }
}
