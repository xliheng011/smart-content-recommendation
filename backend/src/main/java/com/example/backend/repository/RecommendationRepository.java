package com.example.backend.repository;

import com.example.backend.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository
        extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdOrderByScoreDesc(Long userId);

    Optional<Recommendation> findByUserIdAndContentId(Long userId, Long contentId);
}