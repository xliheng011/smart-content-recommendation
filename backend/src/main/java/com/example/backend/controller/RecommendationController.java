package com.example.backend.controller;

import com.example.backend.entity.Recommendation;
import com.example.backend.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(
            RecommendationService recommendationService) {

        this.recommendationService = recommendationService;
    }

    @PostMapping("/{userId}/generate")
    public String generateRecommendations(
            @PathVariable Long userId) {

        recommendationService.generateRecommendations(userId);

        return "推荐生成成功";
    }

    @GetMapping("/{userId}")
    public List<Recommendation> getRecommendations(
            @PathVariable Long userId) {

        return recommendationService.getRecommendations(userId);
    }
}