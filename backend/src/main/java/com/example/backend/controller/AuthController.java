package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.security.CurrentUserHolder;
import com.example.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证相关接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册并直接登录。
     *
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        return ApiResponse.ok(
                "注册成功",
                authService.register(request)
        );
    }

    /**
     * 登录。
     *
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ApiResponse.ok(
                "登录成功",
                authService.login(request)
        );
    }

    /**
     * 注销当前令牌。
     *
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {

        authService.logout(extractToken(authorization));

        return ApiResponse.ok("已退出登录", null);
    }

    /**
     * 获取当前登录用户。
     *
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {

        return ApiResponse.ok(
                authService.currentUser(CurrentUserHolder.get())
        );
    }

    private String extractToken(String authorization) {

        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        if (authorization.regionMatches(
                true,
                0,
                "Bearer ",
                0,
                7
        )) {
            return authorization.substring(7).trim();
        }

        return authorization.trim();
    }
}
