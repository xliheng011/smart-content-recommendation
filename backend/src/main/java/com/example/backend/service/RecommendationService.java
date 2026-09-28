package com.example.backend.service;

import com.example.backend.dto.HotRecommendationResponse;
import com.example.backend.dto.RecommendationResponse;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserBehaviorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 推荐服务。
 *
 * 个性化推荐打分公式：
 *
 *   score = 兴趣权重 * 20          （用户对内容分类的历史偏好）
 *         + min(热度, 500)         （点赞 * 3 + 浏览，做上限截断防止爆款垄断）
 *         + 新鲜度 * 2             （30 天内线性衰减）
 *         - 已读惩罚               （读过 8 分，避免反复推荐同一篇）
 *
 * 已经点赞过的内容不再出现在推荐流里，因为它们已经沉淀到"我的喜欢"。
 */
@Service
public class RecommendationService {

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(10);

    private static final Duration HOT_CACHE_TTL = Duration.ofMinutes(5);

    private static final int RECOMMEND_LIMIT = 12;

    private static final int HOT_LIMIT = 10;

    /** 兴趣权重上限，避免单一分类彻底垄断推荐流 */
    private static final int AFFINITY_CAP = 30;

    private static final int HOT_CAP = 500;

    private static final int FRESHNESS_DAYS = 30;

    private final UserBehaviorRepository userBehaviorRepository;

    private final ContentRepository contentRepository;

    private final ContentMapper contentMapper;

    private final CacheService cacheService;

    public RecommendationService(
            UserBehaviorRepository userBehaviorRepository,
            ContentRepository contentRepository,
            ContentMapper contentMapper,
            CacheService cacheService
    ) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.contentRepository = contentRepository;
        this.contentMapper = contentMapper;
        this.cacheService = cacheService;
    }

    /* ============================
       个性化推荐
    ============================ */

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommend(Long userId) {

        String cacheKey = "recommend:user:" + userId;

        Optional<List<RecommendationResponse>> cached = cacheService.get(
                cacheKey,
                new TypeReference<List<RecommendationResponse>>() {
                }
        );

        if (cached.isPresent()) {
            return cached.get();
        }

        List<UserBehavior> behaviors =
                userBehaviorRepository.findByUserId(userId);

        List<Content> allContents = contentRepository.findAll();

        Map<Long, Content> contentIndex = new HashMap<>();

        allContents.forEach(content ->
                contentIndex.put(content.getId(), content)
        );

        /* 1. 统计分类兴趣分：点赞 3 分，浏览 1 分 */
        Map<String, Integer> categoryScore = new HashMap<>();

        Set<Long> likedContentIds = new HashSet<>();

        Set<Long> viewedContentIds = new HashSet<>();

        for (UserBehavior behavior : behaviors) {

            Content content = contentIndex.get(behavior.getContentId());

            if (content == null) {
                continue;
            }

            if (ContentMapper.BEHAVIOR_LIKE.equals(behavior.getBehaviorType())) {
                likedContentIds.add(behavior.getContentId());
            } else {
                viewedContentIds.add(behavior.getContentId());
            }

            String category = content.getCategory();

            if (category == null || category.isBlank()) {
                continue;
            }

            int weight = ContentMapper.BEHAVIOR_LIKE
                    .equals(behavior.getBehaviorType()) ? 3 : 1;

            categoryScore.merge(category, weight, Integer::sum);
        }

        boolean coldStart = categoryScore.isEmpty();

        /* 2. 计算每个内容的得分 */
        List<RecommendationResponse> result = new ArrayList<>();

        for (Content content : allContents) {

            // 已点赞的内容不再推荐
            if (likedContentIds.contains(content.getId())) {
                continue;
            }

            int affinity = Math.min(
                    categoryScore.getOrDefault(content.getCategory(), 0),
                    AFFINITY_CAP
            );

            int hotness = Math.min(hotScore(content), HOT_CAP);

            long freshness = freshnessBonus(content.getCreatedAt());

            int readPenalty = viewedContentIds.contains(content.getId())
                    ? 8
                    : 0;

            int score = affinity * 20
                    + hotness
                    + (int) freshness * 2
                    - readPenalty;

            RecommendationResponse response = new RecommendationResponse();

            response.setId(content.getId());
            response.setTitle(content.getTitle());
            response.setCategory(content.getCategory());
            response.setScore(score);
            response.setContent(content.getContent());
            response.setPreview(ContentMapper.preview(content.getContent()));
            response.setViewCount(nullSafe(content.getViewCount()));
            response.setLikeCount(nullSafe(content.getLikeCount()));
            response.setLiked(false);
            response.setAuthorId(content.getAuthorId());
            response.setCreatedAt(ContentMapper.format(content.getCreatedAt()));
            response.setReason(buildReason(
                    affinity,
                    hotness,
                    content.getCategory(),
                    coldStart
            ));

            result.add(response);
        }

        result.sort(
                Comparator.comparing(RecommendationResponse::getScore)
                        .reversed()
        );

        List<RecommendationResponse> limited = result.stream()
                .limit(RECOMMEND_LIMIT)
                .toList();

        contentMapper.fillAuthorNamesForRecommendations(limited);

        cacheService.set(cacheKey, limited, USER_CACHE_TTL);

        return limited;
    }

    /* ============================
       热门榜
    ============================ */

    @Transactional(readOnly = true)
    public List<HotRecommendationResponse> hotRecommend(Long userId) {

        String cacheKey = "recommend:hot";

        List<HotRecommendationResponse> ranking;

        Optional<List<HotRecommendationResponse>> cached = cacheService.get(
                cacheKey,
                new TypeReference<List<HotRecommendationResponse>>() {
                }
        );

        if (cached.isPresent()) {
            ranking = cached.get();
        } else {

            List<Content> sorted = contentRepository.findAll()
                    .stream()
                    .sorted(
                            Comparator.comparingInt(
                                    RecommendationService::hotScore
                            ).reversed()
                    )
                    .limit(HOT_LIMIT)
                    .toList();

            List<HotRecommendationResponse> built = new ArrayList<>();

            int rank = 1;

            for (Content content : sorted) {

                HotRecommendationResponse item =
                        new HotRecommendationResponse();

                item.setId(content.getId());
                item.setTitle(content.getTitle());
                item.setCategory(content.getCategory());
                item.setScore(hotScore(content));
                item.setContent(content.getContent());
                item.setPreview(ContentMapper.preview(content.getContent()));
                item.setViewCount(nullSafe(content.getViewCount()));
                item.setLikeCount(nullSafe(content.getLikeCount()));
                item.setLiked(false);
                item.setAuthorId(content.getAuthorId());
                item.setCreatedAt(ContentMapper.format(content.getCreatedAt()));
                item.setRank(rank++);

                built.add(item);
            }

            contentMapper.fillAuthorNamesForHot(built);

            ranking = built;

            cacheService.set(cacheKey, ranking, HOT_CACHE_TTL);
        }

        // liked 是随用户变化的，缓存只存榜单本身
        if (userId != null && !ranking.isEmpty()) {

            Set<Long> likedIds = contentMapper.likedContentIds(
                    userId,
                    ranking.stream().map(HotRecommendationResponse::getId).toList()
            );

            ranking.forEach(item ->
                    item.setLiked(likedIds.contains(item.getId()))
            );
        }

        return ranking;
    }

    /* ============================
       工具方法
    ============================ */

    /**
     * 热度分：点赞权重更高，浏览做辅助。
     */
    private static int hotScore(Content content) {

        int likes = nullSafe(content.getLikeCount());

        int views = nullSafe(content.getViewCount());

        return likes * 3 + views;
    }

    /**
     * 新鲜度：FRESHNESS_DAYS 天内线性衰减，越新分越高。
     */
    private static long freshnessBonus(LocalDateTime createdAt) {

        if (createdAt == null) {
            return 0;
        }

        long days = ChronoUnit.DAYS.between(
                createdAt,
                LocalDateTime.now()
        );

        if (days < 0) {
            days = 0;
        }

        if (days >= FRESHNESS_DAYS) {
            return 0;
        }

        return FRESHNESS_DAYS - days;
    }

    private static String buildReason(
            int affinity,
            int hotness,
            String category,
            boolean coldStart
    ) {

        if (coldStart) {
            return "热门推荐";
        }

        if (affinity > 0 && category != null && !category.isBlank()) {
            return "因为你喜欢「" + category + "」";
        }

        if (hotness >= 60) {
            return "近期热门";
        }

        return "为你发现";
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
