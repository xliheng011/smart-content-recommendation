package com.example.backend.service;

import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.UserBehaviorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserBehaviorService {

    private final UserBehaviorRepository userBehaviorRepository;

    public UserBehaviorService(
            UserBehaviorRepository userBehaviorRepository) {
        this.userBehaviorRepository = userBehaviorRepository;
    }

    public UserBehavior recordBehavior(
            Long userId,
            Long contentId,
            String behaviorType) {

        UserBehavior behavior = new UserBehavior();

        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setBehaviorType(behaviorType);

        return userBehaviorRepository.save(behavior);
    }

    public List<UserBehavior> getUserBehaviors(Long userId) {
        return userBehaviorRepository.findByUserId(userId);
    }
}