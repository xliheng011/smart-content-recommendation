package com.example.backend.service;

import com.example.backend.common.ApiException;
import com.example.backend.dto.BehaviorResponse;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserBehaviorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserBehaviorService {

    private static final Set<String> SUPPORTED_TYPES =
            Set.of("VIEW", "LIKE");

    /** 最近互动列表最多返回多少条 */
    private static final int RECENT_MAX = 20;

    private final UserBehaviorRepository userBehaviorRepository;

    private final ContentRepository contentRepository;

    public UserBehaviorService(
            UserBehaviorRepository userBehaviorRepository,
            ContentRepository contentRepository
    ) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * 记录一次用户行为。
     *
     * 浏览行为允许重复（用于兴趣加权），点赞由调用方保证幂等。
     */
    @Transactional
    public UserBehavior recordBehavior(
            Long userId,
            Long contentId,
            String behaviorType
    ) {

        if (userId == null || contentId == null) {
            throw ApiException.badRequest("用户与内容不能为空");
        }

        String type = normalizeType(behaviorType);

        UserBehavior behavior = new UserBehavior();

        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setBehaviorType(type);

        return userBehaviorRepository.save(behavior);
    }

    public boolean hasBehavior(
            Long userId,
            Long contentId,
            String behaviorType
    ) {

        if (userId == null || contentId == null) {
            return false;
        }

        return userBehaviorRepository
                .existsByUserIdAndContentIdAndBehaviorType(
                        userId,
                        contentId,
                        normalizeType(behaviorType)
                );
    }

    @Transactional
    public void removeBehavior(
            Long userId,
            Long contentId,
            String behaviorType
    ) {

        if (userId == null || contentId == null) {
            return;
        }

        userBehaviorRepository.deleteByUserIdAndContentIdAndBehaviorType(
                userId,
                contentId,
                normalizeType(behaviorType)
        );
    }

    /**
     * 用户点赞过的内容 ID。
     */
    @Transactional(readOnly = true)
    public Set<Long> likedContentIds(Long userId) {

        if (userId == null) {
            return Set.of();
        }

        return userBehaviorRepository
                .findByUserIdAndBehaviorType(userId, "LIKE")
                .stream()
                .map(UserBehavior::getContentId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<BehaviorResponse> getUserBehaviors(Long userId) {

        return toResponses(
                userBehaviorRepository.findTop30ByUserIdOrderByIdDesc(userId)
        );
    }

    /**
     * 最近互动过的内容，按内容去重、保留最近一次。
     *
     * 浏览行为是"每看一次记一条"，直接铺开会满屏重复条目，
     * 个人中心的动态面板用这个版本更可读。
     */
    @Transactional(readOnly = true)
    public List<BehaviorResponse> getRecentBehaviors(
            Long userId,
            int limit
    ) {

        if (userId == null) {
            return List.of();
        }

        int safeLimit = Math.min(Math.max(limit, 1), RECENT_MAX);

        Map<Long, UserBehavior> deduped = new LinkedHashMap<>();

        for (UserBehavior behavior : userBehaviorRepository
                .findTop100ByUserIdOrderByIdDesc(userId)) {

            if (behavior.getContentId() == null) {
                continue;
            }

            deduped.putIfAbsent(behavior.getContentId(), behavior);

            if (deduped.size() >= safeLimit) {
                break;
            }
        }

        return toResponses(List.copyOf(deduped.values()));
    }

    /**
     * 取某用户某类行为的原始记录，按时间倒序。
     * 阅读历史需要自己按内容去重，所以这里返回实体而不是 DTO。
     */
    @Transactional(readOnly = true)
    public List<UserBehavior> getBehaviors(
            Long userId,
            String behaviorType
    ) {

        if (userId == null) {
            return List.of();
        }

        return userBehaviorRepository
                .findByUserIdAndBehaviorTypeOrderByIdDesc(
                        userId,
                        normalizeType(behaviorType)
                );
    }

    /**
     * 内容被删除时清理其关联行为，避免统计口径出现幽灵数据。
     */
    @Transactional
    public void removeByContent(Long contentId) {

        if (contentId == null) {
            return;
        }

        userBehaviorRepository.deleteByContentId(contentId);
    }

    public long countByUser(Long userId) {
        return userBehaviorRepository.countByUserId(userId);
    }

    public long countDistinctContent(Long userId, String behaviorType) {
        return userBehaviorRepository.countDistinctContent(
                userId,
                normalizeType(behaviorType)
        );
    }

    /**
     * 实体转 DTO，并批量补齐内容标题，避免 N+1。
     */
    private List<BehaviorResponse> toResponses(
            List<UserBehavior> behaviors
    ) {

        if (behaviors.isEmpty()) {
            return List.of();
        }

        Set<Long> contentIds = behaviors.stream()
                .map(UserBehavior::getContentId)
                .collect(Collectors.toSet());

        Map<Long, Content> contents = new HashMap<>();

        contentRepository.findAllById(contentIds).forEach(content ->
                contents.put(content.getId(), content)
        );

        return behaviors.stream()
                .map(behavior -> {

                    BehaviorResponse response = new BehaviorResponse();

                    response.setId(behavior.getId());
                    response.setUserId(behavior.getUserId());
                    response.setContentId(behavior.getContentId());
                    response.setBehaviorType(behavior.getBehaviorType());
                    response.setCreatedAt(
                            ContentMapper.format(behavior.getCreatedAt())
                    );

                    Content content = contents.get(behavior.getContentId());

                    if (content != null) {
                        response.setContentTitle(content.getTitle());
                        response.setCategory(content.getCategory());
                    } else {
                        response.setContentTitle("内容已删除");
                    }

                    return response;
                })
                .toList();
    }

    private String normalizeType(String behaviorType) {

        if (behaviorType == null || behaviorType.isBlank()) {
            throw ApiException.badRequest("行为类型不能为空");
        }

        String type = behaviorType.trim().toUpperCase();

        if (!SUPPORTED_TYPES.contains(type)) {
            throw ApiException.badRequest(
                    "不支持的行为类型：" + behaviorType
            );
        }

        return type;
    }
}
