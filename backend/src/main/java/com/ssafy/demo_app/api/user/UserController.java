package com.ssafy.demo_app.api.user;

import com.ssafy.demo_app.api.user.dto.UserResponse;
import com.ssafy.demo_app.api.user.dto.UserRoleUpdateRequest;
import com.ssafy.demo_app.api.user.dto.UserUpdateRequest;
import com.ssafy.demo_app.global.response.ApiResponse;
import com.ssafy.demo_app.infrastructure.security.details.CustomUserDetails;
import com.ssafy.demo_app.domain.user.service.UserService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyInfo(userDetails.getUserId())));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("사용자 정보가 수정되었습니다.",
                userService.updateMyInfo(userDetails.getUserId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsers()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUser(userId)));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable Integer userId,
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("사용자 권한이 수정되었습니다.",
                userService.updateRole(userId, request)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 삭제되었습니다."));
    }
}
