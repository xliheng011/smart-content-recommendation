package com.example.backend.service;

import com.example.backend.dto.HotRecommendationResponse;
import com.example.backend.dto.RecommendationResponse;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserBehaviorRepository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final ContentRepository contentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecommendationService(
            UserBehaviorRepository userBehaviorRepository,
            ContentRepository contentRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.contentRepository = contentRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 个性化推荐
     *
     * GET /api/recommend/{userId}
     */
    public List<RecommendationResponse> recommend(Long userId) {

        String cacheKey = "recommend:user:" + userId;

        Object cache = redisTemplate
                .opsForValue()
                .get(cacheKey);

        if (cache instanceof List<?> list) {

            System.out.println(
                    "推荐缓存命中: " + cacheKey
            );

            return (List<RecommendationResponse>) list;
        }

        System.out.println(
                "推荐缓存未命中: " + cacheKey
        );

        /*
         * 读取用户行为
         */
        List<UserBehavior> behaviors =
                userBehaviorRepository.findByUserId(userId);

        /*
         * 计算用户对不同分类的兴趣分数
         *
         * VIEW = 1 分
         * LIKE = 3 分
         */
        Map<String, Integer> categoryScore =
                new HashMap<>();

        for (UserBehavior behavior : behaviors) {

            Content content =
                    contentRepository
                            .findById(behavior.getContentId())
                            .orElse(null);

            if (content == null) {
                continue;
            }

            String category = content.getCategory();

            if (category == null) {
                continue;
            }

            int score =
                    "LIKE".equals(behavior.getBehaviorType())
                            ? 3
                            : 1;

            categoryScore.merge(
                    category,
                    score,
                    Integer::sum
            );
        }

        /*
         * 生成推荐列表
         */
        List<RecommendationResponse> result =
                contentRepository.findAll()
                        .stream()
                        .map(content -> {

                            int score =
                                    categoryScore.getOrDefault(
                                            content.getCategory(),
                                            0
                                    );

                            /*
                             * 这里使用新的 7 参数构造函数
                             *
                             * id
                             * title
                             * category
                             * score
                             * content
                             * viewCount
                             * likeCount
                             */
                            return new RecommendationResponse(
                                    content.getId(),
                                    content.getTitle(),
                                    content.getCategory(),
                                    score,
                                    content.getContent(),
                                    content.getViewCount(),
                                    content.getLikeCount()
                            );
                        })
                        .sorted(
                                Comparator.comparing(
                                        RecommendationResponse::getScore
                                ).reversed()
                        )
                        .limit(10)
                        .collect(Collectors.toList());

        /*
         * 写入 Redis
         */
        redisTemplate
                .opsForValue()
                .set(cacheKey, result);

        System.out.println(
                "推荐缓存写入: " + cacheKey
        );

        return result;
    }

    /**
     * 热门推荐
     *
     * GET /api/recommend/hot
     */
    public List<HotRecommendationResponse> hotRecommend() {

        String cacheKey = "recommend:hot";

        Object cache =
                redisTemplate
                        .opsForValue()
                        .get(cacheKey);

        if (cache instanceof List<?> list) {

            System.out.println(
                    "热门推荐缓存命中"
            );

            return (List<HotRecommendationResponse>) list;
        }

        System.out.println(
                "热门推荐缓存未命中"
        );

        /*
         * 热门分数：
         *
         * 浏览量 + 点赞量
         */
        List<HotRecommendationResponse> result =
                contentRepository.findAll()
                        .stream()
                        .map(content -> {

                            int hotScore =
                                    content.getLikeCount()
                                            + content.getViewCount();

                            return new HotRecommendationResponse(
                                    content.getId(),
                                    content.getTitle(),
                                    content.getCategory(),
                                    hotScore
                            );
                        })
                        .sorted(
                                Comparator.comparing(
                                        HotRecommendationResponse::getScore
                                ).reversed()
                        )
                        .limit(10)
                        .collect(Collectors.toList());

        /*
         * 写入 Redis
         */
        redisTemplate
                .opsForValue()
                .set(cacheKey, result);

        System.out.println(
                "热门推荐缓存写入"
        );

        return result;
    }
}