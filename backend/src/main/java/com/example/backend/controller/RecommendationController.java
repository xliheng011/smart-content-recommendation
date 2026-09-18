package com.example.backend.controller;


import com.example.backend.dto.HotRecommendationResponse;
import com.example.backend.dto.RecommendationResponse;
import com.example.backend.service.RecommendationService;

import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {


    private final RecommendationService recommendationService;


    public RecommendationController(
            RecommendationService recommendationService
    ){
        this.recommendationService = recommendationService;
    }



    /**
     * 用户个性化推荐
     *
     * GET:
     * /api/recommend/7
     */
    @GetMapping("/{userId}")
    public List<RecommendationResponse> recommend(
            @PathVariable Long userId
    ){

        return recommendationService.recommend(userId);

    }



    /**
     * 热门推荐
     *
     * GET:
     * /api/recommend/hot
     */
    @GetMapping("/hot")
    public List<HotRecommendationResponse> hot(){

        return recommendationService.hotRecommend();

    }


}