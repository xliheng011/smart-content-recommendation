package com.example.backend.controller;

import com.example.backend.common.ApiException;
import com.example.backend.common.ApiResponse;
import com.example.backend.dto.UserResponse;
import com.example.backend.dto.UserStatsResponse;
import com.example.backend.security.CurrentUserHolder;
import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户相关接口。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 当前登录用户。
     *
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {

        Long userId = CurrentUserHolder.getUserId();

        if (userId == null) {
            throw ApiException.unauthorized("尚未登录");
        }

        return ApiResponse.ok(userService.getUserById(userId));
    }

    /**
     * 当前登录用户的统计。
     *
     * GET /api/users/me/stats
     */
    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> myStats() {

        Long userId = CurrentUserHolder.getUserId();

        if (userId == null) {
            throw ApiException.unauthorized("尚未登录");
        }

        return ApiResponse.ok(userService.getUserStats(userId));
    }

    /**
     * 指定用户统计。
     *
     * GET /api/users/7/stats
     */
    @GetMapping("/{id}/stats")
    public ApiResponse<UserStatsResponse> stats(@PathVariable Long id) {

        return ApiResponse.ok(userService.getUserStats(id));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {

        return ApiResponse.ok(userService.getUserById(id));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {

        return ApiResponse.ok(userService.getAllUsers());
    }
}
