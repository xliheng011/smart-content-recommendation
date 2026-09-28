package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.HotRecommendationResponse;
import com.example.backend.dto.RecommendationResponse;
import com.example.backend.security.CurrentUserHolder;
import com.example.backend.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐接口。
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(
            RecommendationService recommendationService
    ) {
        this.recommendationService = recommendationService;
    }

    /**
     * 热门榜单（无需登录，登录后额外返回 liked 状态）。
     *
     * GET /api/recommend/hot
     */
    @GetMapping("/hot")
    public ApiResponse<List<HotRecommendationResponse>> hot() {

        return ApiResponse.ok(
                recommendationService.hotRecommend(
                        CurrentUserHolder.getUserId()
                )
        );
    }

    /**
     * 个性化推荐。
     *
     * GET /api/recommend/me   （使用当前登录用户）
     * GET /api/recommend/7    （指定用户，便于调试）
     */
    @GetMapping("/me")
    public ApiResponse<List<RecommendationResponse>> recommendForMe() {

        Long userId = CurrentUserHolder.getUserId();

        if (userId == null) {
            throw com.example.backend.common.ApiException
                    .unauthorized("请先登录后查看个性化推荐");
        }

        return ApiResponse.ok(
                recommendationService.recommend(userId)
        );
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<RecommendationResponse>> recommend(
            @PathVariable Long userId
    ) {

        return ApiResponse.ok(
                recommendationService.recommend(userId)
        );
    }
}
