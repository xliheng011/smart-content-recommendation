package com.example.backend.controller;

import com.example.backend.common.ApiException;
import com.example.backend.common.ApiResponse;
import com.example.backend.dto.BehaviorResponse;
import com.example.backend.security.CurrentUserHolder;
import com.example.backend.service.UserBehaviorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户行为接口。
 */
@RestController
@RequestMapping("/api/behaviors")
public class UserBehaviorController {

    private final UserBehaviorService userBehaviorService;

    public UserBehaviorController(
            UserBehaviorService userBehaviorService
    ) {
        this.userBehaviorService = userBehaviorService;
    }

    /**
     * 上报行为。
     *
     * POST /api/behaviors?userId=1&contentId=1&behaviorType=VIEW
     */
    @PostMapping
    public ApiResponse<BehaviorResponse> recordBehavior(
            @RequestParam(required = false) Long userId,
            @RequestParam Long contentId,
            @RequestParam String behaviorType
    ) {

        Long effectiveUserId = userId != null
                ? userId
                : CurrentUserHolder.getUserId();

        if (effectiveUserId == null) {
            throw ApiException.unauthorized("请先登录或指定 userId");
        }

        userBehaviorService.recordBehavior(
                effectiveUserId,
                contentId,
                behaviorType
        );

        return ApiResponse.ok("行为已记录", null);
    }

    /**
     * 查询某用户的行为。
     *
     * GET /api/behaviors/user/1
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<BehaviorResponse>> getUserBehaviors(
            @PathVariable Long userId
    ) {

        return ApiResponse.ok(
                userBehaviorService.getUserBehaviors(userId)
        );
    }
}
