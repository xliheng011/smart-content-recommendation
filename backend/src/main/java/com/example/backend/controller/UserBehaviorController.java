package com.example.backend.controller;

import com.example.backend.entity.UserBehavior;
import com.example.backend.service.UserBehaviorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/behaviors")
public class UserBehaviorController {

    private final UserBehaviorService userBehaviorService;

    public UserBehaviorController(
            UserBehaviorService userBehaviorService) {
        this.userBehaviorService = userBehaviorService;
    }

    @PostMapping
    public UserBehavior recordBehavior(
            @RequestParam Long userId,
            @RequestParam Long contentId,
            @RequestParam String behaviorType) {

        return userBehaviorService.recordBehavior(
                userId,
                contentId,
                behaviorType
        );
    }

    @GetMapping("/user/{userId}")
    public List<UserBehavior> getUserBehaviors(
            @PathVariable Long userId) {

        return userBehaviorService.getUserBehaviors(userId);
    }
}