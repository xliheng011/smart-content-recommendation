package com.example.backend.repository;

import com.example.backend.entity.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBehaviorRepository
        extends JpaRepository<UserBehavior, Long> {

    List<UserBehavior> findByUserId(Long userId);

    List<UserBehavior> findByContentId(Long contentId);

    List<UserBehavior> findByUserIdAndBehaviorType(
            Long userId,
            String behaviorType
    );
}