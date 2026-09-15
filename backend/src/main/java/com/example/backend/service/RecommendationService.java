package com.example.backend.service;

import com.example.backend.entity.Recommendation;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.RecommendationRepository;
import com.example.backend.repository.UserBehaviorRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final UserBehaviorRepository userBehaviorRepository;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            UserBehaviorRepository userBehaviorRepository) {

        this.recommendationRepository = recommendationRepository;
        this.userBehaviorRepository = userBehaviorRepository;
    }

    public List<Recommendation> getRecommendations(Long userId) {

        return recommendationRepository
                .findByUserIdOrderByScoreDesc(userId);
    }

    public void generateRecommendations(Long userId) {

        List<UserBehavior> behaviors =
                userBehaviorRepository.findByUserId(userId);

        Map<Long, Double> contentScores = new HashMap<>();

        for (UserBehavior behavior : behaviors) {

            Long contentId = behavior.getContentId();

            double score = 0;

            if ("VIEW".equals(behavior.getBehaviorType())) {
                score = 1;
            } else if ("LIKE".equals(behavior.getBehaviorType())) {
                score = 3;
            }

            contentScores.merge(
                    contentId,
                    score,
                    Double::sum
            );
        }

        for (Map.Entry<Long, Double> entry : contentScores.entrySet()) {

            Recommendation recommendation =
                    recommendationRepository
                            .findByUserIdAndContentId(userId, entry.getKey())
                            .orElseGet(Recommendation::new);

            recommendation.setUserId(userId);
            recommendation.setContentId(entry.getKey());
            recommendation.setScore(entry.getValue());
            recommendation.setReason("根据用户行为推荐");

            recommendationRepository.save(recommendation);
        }
    }
}