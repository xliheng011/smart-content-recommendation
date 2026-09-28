package com.example.backend.service;

import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.HotRecommendationResponse;
import com.example.backend.dto.RecommendationResponse;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.UserBehaviorRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内容实体 -> DTO 的装配逻辑，集中放在这里避免各 Service 重复。
 */
@Component
public class ContentMapper {

    public static final String BEHAVIOR_VIEW = "VIEW";

    public static final String BEHAVIOR_LIKE = "LIKE";

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int PREVIEW_LENGTH = 86;

    private static final String ANONYMOUS = "匿名作者";

    private final UserRepository userRepository;

    private final UserBehaviorRepository userBehaviorRepository;

    public ContentMapper(
            UserRepository userRepository,
            UserBehaviorRepository userBehaviorRepository
    ) {
        this.userRepository = userRepository;
        this.userBehaviorRepository = userBehaviorRepository;
    }

    public ContentResponse toResponse(Content content) {

        ContentResponse response = new ContentResponse();

        response.setId(content.getId());
        response.setTitle(content.getTitle());
        response.setContent(content.getContent());
        response.setPreview(preview(content.getContent()));
        response.setCategory(content.getCategory());
        response.setAuthorId(content.getAuthorId());
        response.setViewCount(nullSafe(content.getViewCount()));
        response.setLikeCount(nullSafe(content.getLikeCount()));
        response.setLiked(false);
        response.setCreatedAt(format(content.getCreatedAt()));
        response.setUpdatedAt(format(content.getUpdatedAt()));

        return response;
    }

    public List<ContentResponse> toResponseList(List<Content> contents) {

        List<ContentResponse> list = contents.stream()
                .map(this::toResponse)
                .toList();

        fillAuthorNames(list);

        return list;
    }

    /**
     * 批量补齐作者昵称，避免列表页 N+1 查询。
     */
    public void fillAuthorNames(List<ContentResponse> responses) {

        Map<Long, String> names = authorNameMap(
                responses.stream()
                        .map(ContentResponse::getAuthorId)
                        .toList()
        );

        responses.forEach(response ->
                response.setAuthorName(
                        names.getOrDefault(
                                response.getAuthorId(),
                                ANONYMOUS
                        )
                )
        );
    }

    /**
     * 推荐列表的作者名回填。
     */
    public void fillAuthorNamesForRecommendations(
            List<RecommendationResponse> responses
    ) {

        Map<Long, String> names = authorNameMap(
                responses.stream()
                        .map(RecommendationResponse::getAuthorId)
                        .toList()
        );

        responses.forEach(response ->
                response.setAuthorName(
                        names.getOrDefault(
                                response.getAuthorId(),
                                ANONYMOUS
                        )
                )
        );
    }

    /**
     * 热门榜的作者名回填。
     */
    public void fillAuthorNamesForHot(
            List<HotRecommendationResponse> responses
    ) {

        Map<Long, String> names = authorNameMap(
                responses.stream()
                        .map(HotRecommendationResponse::getAuthorId)
                        .toList()
        );

        responses.forEach(response ->
                response.setAuthorName(
                        names.getOrDefault(
                                response.getAuthorId(),
                                ANONYMOUS
                        )
                )
        );
    }

    /**
     * 批量查询作者展示名。
     */
    private Map<Long, String> authorNameMap(
            Collection<Long> authorIds
    ) {

        Set<Long> ids = authorIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> names = new HashMap<>();

        userRepository.findAllById(ids).forEach(user ->
                names.put(
                        user.getId(),
                        displayName(
                                user.getNickname(),
                                user.getUsername()
                        )
                )
        );

        return names;
    }

    /**
     * 查询当前用户对一批内容的点赞集合。
     */
    public Set<Long> likedContentIds(
            Long userId,
            Collection<Long> contentIds
    ) {

        if (userId == null || contentIds == null || contentIds.isEmpty()) {
            return Set.of();
        }

        return userBehaviorRepository
                .findByUserIdAndContentIdInAndBehaviorType(
                        userId,
                        contentIds,
                        BEHAVIOR_LIKE
                )
                .stream()
                .map(UserBehavior::getContentId)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 批量回填 liked 字段。
     */
    public void fillLiked(
            List<ContentResponse> responses,
            Long userId
    ) {

        if (responses.isEmpty()) {
            return;
        }

        List<Long> ids = responses.stream()
                .map(ContentResponse::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Set<Long> liked = likedContentIds(userId, ids);

        responses.forEach(response ->
                response.setLiked(liked.contains(response.getId()))
        );
    }

    public static String preview(String content) {

        if (content == null) {
            return "";
        }

        String flat = content
                .replaceAll("\\s+", " ")
                .trim();

        if (flat.length() <= PREVIEW_LENGTH) {
            return flat;
        }

        return flat.substring(0, PREVIEW_LENGTH) + "…";
    }

    public static String displayName(String nickname, String username) {

        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }

        if (username != null && !username.isBlank()) {
            return username;
        }

        return "匿名作者";
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : DATE_TIME.format(time);
    }

    private static int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
