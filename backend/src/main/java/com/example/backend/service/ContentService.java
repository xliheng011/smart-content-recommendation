package com.example.backend.service;

import com.example.backend.common.ApiException;
import com.example.backend.common.PageResult;
import com.example.backend.dto.CategoryStatResponse;
import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.CreateContentRequest;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.ContentRepository;
import com.example.backend.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import jakarta.persistence.criteria.Predicate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class ContentService {

    /** 详情缓存有效期，避免浏览量被长时间冻结 */
    private static final Duration DETAIL_TTL = Duration.ofMinutes(2);

    private static final Duration CATEGORY_TTL = Duration.ofMinutes(5);

    private static final int MAX_PAGE_SIZE = 50;

    /** 阅读历史最多返回多少条 */
    private static final int HISTORY_MAX = 50;

    private final ContentRepository contentRepository;

    private final UserBehaviorService userBehaviorService;

    private final ContentMapper contentMapper;

    private final CacheService cacheService;

    public ContentService(
            ContentRepository contentRepository,
            UserBehaviorService userBehaviorService,
            ContentMapper contentMapper,
            CacheService cacheService
    ) {
        this.contentRepository = contentRepository;
        this.userBehaviorService = userBehaviorService;
        this.contentMapper = contentMapper;
        this.cacheService = cacheService;
    }

    /* ============================
       查询
    ============================ */

    /**
     * 分页 + 分类 + 关键字 + 排序 的内容列表。
     */
    @Transactional(readOnly = true)
    public PageResult<ContentResponse> search(
            String category,
            String keyword,
            String sort,
            int page,
            int size,
            Long currentUserId
    ) {

        int safePage = Math.max(page, 1);

        int safeSize = Math.min(
                Math.max(size, 1),
                MAX_PAGE_SIZE
        );

        Pageable pageable = PageRequest.of(
                safePage - 1,
                safeSize,
                resolveSort(sort)
        );

        Page<Content> result = contentRepository.findAll(
                buildSpecification(category, keyword),
                pageable
        );

        List<ContentResponse> items = result.getContent()
                .stream()
                .map(contentMapper::toResponse)
                .toList();

        contentMapper.fillAuthorNames(items);
        contentMapper.fillLiked(items, currentUserId);

        return PageResult.of(
                items,
                result.getTotalElements(),
                safePage,
                safeSize
        );
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> getAllContents(Long currentUserId) {

        List<ContentResponse> items = contentRepository.findAll()
                .stream()
                .map(contentMapper::toResponse)
                .toList();

        contentMapper.fillAuthorNames(items);
        contentMapper.fillLiked(items, currentUserId);

        return items;
    }

    /**
     * 详情：命中缓存时依然会累加浏览量，保证计数不丢。
     */
    @Transactional
    public ContentResponse getContentById(Long id, Long userId) {

        String cacheKey = detailKey(id);

        Optional<ContentResponse> cached = cacheService.get(
                cacheKey,
                ContentResponse.class
        );

        if (cached.isPresent()) {

            ContentResponse response = cached.get();

            contentRepository.incrementViewCount(id);

            response.setViewCount(
                    (response.getViewCount() == null
                            ? 0
                            : response.getViewCount()) + 1
            );

            recordView(userId, id);

            contentMapper.fillLiked(List.of(response), userId);

            return response;
        }

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("内容不存在"));

        contentRepository.incrementViewCount(id);

        recordView(userId, id);

        ContentResponse response = contentMapper.toResponse(content);

        response.setViewCount(
                (response.getViewCount() == null
                        ? 0
                        : response.getViewCount()) + 1
        );

        contentMapper.fillAuthorNames(List.of(response));
        contentMapper.fillLiked(List.of(response), userId);

        cacheService.set(cacheKey, response, DETAIL_TTL);

        return response;
    }

    /**
     * 同分类相关阅读。
     */
    @Transactional(readOnly = true)
    public List<ContentResponse> getSimilar(
            Long id,
            int limit,
            Long currentUserId
    ) {

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("内容不存在"));

        if (content.getCategory() == null) {
            return List.of();
        }

        List<ContentResponse> items = contentRepository
                .findByCategoryAndIdNot(
                        content.getCategory(),
                        id,
                        PageRequest.of(0, Math.max(limit, 1))
                )
                .stream()
                .map(contentMapper::toResponse)
                .toList();

        contentMapper.fillAuthorNames(items);
        contentMapper.fillLiked(items, currentUserId);

        return items;
    }

    /**
     * 分类聚合，供前端筛选器使用。
     */
    public List<CategoryStatResponse> getCategories() {

        String cacheKey = "content:categories";

        Optional<List<CategoryStatResponse>> cached = cacheService.get(
                cacheKey,
                new TypeReference<List<CategoryStatResponse>>() {
                }
        );

        if (cached.isPresent()) {
            return cached.get();
        }

        List<CategoryStatResponse> categories = contentRepository
                .countGroupByCategory()
                .stream()
                .map(row -> new CategoryStatResponse(
                        row.getName(),
                        row.getTotal()
                ))
                .toList();

        cacheService.set(cacheKey, categories, CATEGORY_TTL);

        return categories;
    }

    /* ============================
       写入
    ============================ */

    @Transactional
    public ContentResponse createContent(
            CreateContentRequest request,
            AuthUser actor
    ) {

        if (actor == null) {
            throw ApiException.unauthorized("请先登录后再发布");
        }

        /*
         * 默认以自己的身份发布。
         * authorId 只允许管理员用来"代他人发布"——普通用户传了也会被拒绝，
         * 否则任何人都能伪造作者发帖。
         */
        Long authorId = actor.userId();

        if (request.getAuthorId() != null
                && !request.getAuthorId().equals(actor.userId())) {

            if (!actor.isAdmin()) {
                throw ApiException.forbidden("只能以自己的身份发布内容");
            }

            authorId = request.getAuthorId();
        }

        if (authorId == null) {
            throw ApiException.badRequest("缺少作者信息");
        }

        Content content = new Content();

        content.setTitle(request.getTitle().trim());
        content.setContent(request.getContent());
        content.setCategory(request.getCategory().trim());
        content.setAuthorId(authorId);
        content.setViewCount(0);
        content.setLikeCount(0);

        LocalDateTime now = LocalDateTime.now();

        content.setCreatedAt(now);
        content.setUpdatedAt(now);

        Content saved = contentRepository.save(content);

        // 新增内容会影响分类统计与热门榜
        cacheService.delete("content:categories");
        cacheService.delete("recommend:hot");

        ContentResponse response = contentMapper.toResponse(saved);

        contentMapper.fillAuthorNames(List.of(response));
        contentMapper.fillLiked(List.of(response), actor.userId());

        return response;
    }

    /**
     * 我发布的内容，最新在前。
     */
    @Transactional(readOnly = true)
    public List<ContentResponse> getMyContents(Long userId) {

        if (userId == null) {
            return List.of();
        }

        List<ContentResponse> items = contentRepository
                .findByAuthorIdOrderByIdDesc(userId)
                .stream()
                .map(contentMapper::toResponse)
                .toList();

        contentMapper.fillAuthorNames(items);
        contentMapper.fillLiked(items, userId);

        return items;
    }

    /**
     * 阅读历史。
     *
     * 浏览行为是"每看一次记一条"，所以这里需要按内容去重，
     * 只保留最近一次阅读时间，再按时间倒序返回。
     */
    @Transactional(readOnly = true)
    public List<ContentResponse> getViewedContents(
            Long userId,
            int limit
    ) {

        if (userId == null) {
            return List.of();
        }

        int safeLimit = Math.min(Math.max(limit, 1), HISTORY_MAX);

        List<UserBehavior> views = userBehaviorService
                .getBehaviors(userId, ContentMapper.BEHAVIOR_VIEW);

        if (views.isEmpty()) {
            return List.of();
        }

        // contentId -> 最近一次阅读时间，LinkedHashMap 保住"最近在前"的顺序
        Map<Long, String> viewedAt = new LinkedHashMap<>();

        for (UserBehavior view : views) {

            if (view.getContentId() == null) {
                continue;
            }

            viewedAt.putIfAbsent(
                    view.getContentId(),
                    ContentMapper.format(view.getCreatedAt())
            );

            if (viewedAt.size() >= safeLimit) {
                break;
            }
        }

        Map<Long, Content> index = new HashMap<>();

        contentRepository.findAllById(viewedAt.keySet())
                .forEach(content -> index.put(content.getId(), content));

        List<ContentResponse> items = new ArrayList<>();

        viewedAt.forEach((contentId, time) -> {

            Content content = index.get(contentId);

            // 内容可能已被作者或管理员删除，直接跳过
            if (content == null) {
                return;
            }

            ContentResponse response = contentMapper.toResponse(content);

            response.setViewedAt(time);

            items.add(response);
        });

        contentMapper.fillAuthorNames(items);
        contentMapper.fillLiked(items, userId);

        return items;
    }

    /**
     * 删除内容。
     *
     * 管理员可删任意内容；普通用户只能删自己发布的。
     * 同时清理关联行为记录，避免统计口径出现"幽灵数据"。
     */
    @Transactional
    public void deleteContent(Long id, AuthUser actor) {

        if (actor == null) {
            throw ApiException.unauthorized("请先登录");
        }

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("内容不存在"));

        boolean owner = Objects.equals(
                content.getAuthorId(),
                actor.userId()
        );

        if (!owner && !actor.isAdmin()) {
            throw ApiException.forbidden("只能删除自己发布的内容");
        }

        userBehaviorService.removeByContent(id);

        contentRepository.deleteById(id);

        cacheService.delete(detailKey(id));
        cacheService.delete("content:categories");
        cacheService.delete("recommend:hot");
    }

    /**
     * 点赞（幂等）。
     */
    @Transactional
    public ContentResponse likeContent(Long id, Long userId) {

        if (userId == null) {
            throw ApiException.unauthorized("请先登录后再点赞");
        }

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("内容不存在"));

        boolean alreadyLiked = userBehaviorService
                .hasBehavior(userId, id, ContentMapper.BEHAVIOR_LIKE);

        if (!alreadyLiked) {

            contentRepository.incrementLikeCount(id);

            userBehaviorService.recordBehavior(
                    userId,
                    id,
                    ContentMapper.BEHAVIOR_LIKE
            );

            evictAfterInteraction(id, userId);

            content.setLikeCount(
                    (content.getLikeCount() == null
                            ? 0
                            : content.getLikeCount()) + 1
            );
        }

        ContentResponse response = contentMapper.toResponse(content);

        response.setLiked(true);

        contentMapper.fillAuthorNames(List.of(response));

        return response;
    }

    /**
     * 取消点赞（幂等）。
     */
    @Transactional
    public ContentResponse unlikeContent(Long id, Long userId) {

        if (userId == null) {
            throw ApiException.unauthorized("请先登录后再操作");
        }

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("内容不存在"));

        boolean liked = userBehaviorService
                .hasBehavior(userId, id, ContentMapper.BEHAVIOR_LIKE);

        if (liked) {

            contentRepository.decrementLikeCount(id);

            userBehaviorService.removeBehavior(
                    userId,
                    id,
                    ContentMapper.BEHAVIOR_LIKE
            );

            evictAfterInteraction(id, userId);

            content.setLikeCount(
                    Math.max(
                            (content.getLikeCount() == null
                                    ? 0
                                    : content.getLikeCount()) - 1,
                            0
                    )
            );
        }

        ContentResponse response = contentMapper.toResponse(content);

        response.setLiked(false);

        contentMapper.fillAuthorNames(List.of(response));

        return response;
    }

    /**
     * 查询当前用户点赞过的内容。
     */
    @Transactional(readOnly = true)
    public List<ContentResponse> getLikedContents(Long userId) {

        Set<Long> ids = userBehaviorService
                .likedContentIds(userId);

        if (ids.isEmpty()) {
            return List.of();
        }

        List<ContentResponse> items = contentRepository
                .findAllById(ids)
                .stream()
                .map(contentMapper::toResponse)
                .toList();

        contentMapper.fillAuthorNames(items);
        contentMapper.fillLiked(items, userId);

        return items;
    }

    /* ============================
       内部工具
    ============================ */

    private void recordView(Long userId, Long contentId) {

        if (userId == null) {
            return;
        }

        userBehaviorService.recordBehavior(
                userId,
                contentId,
                ContentMapper.BEHAVIOR_VIEW
        );
    }

    private void evictAfterInteraction(Long contentId, Long userId) {

        cacheService.delete(detailKey(contentId));
        cacheService.delete("recommend:user:" + userId);
        cacheService.delete("recommend:hot");
    }

    private String detailKey(Long id) {
        return "content:detail:" + id;
    }

    private Sort resolveSort(String sort) {

        if (sort == null || sort.isBlank()) {
            return Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        }

        return switch (sort.trim().toLowerCase()) {

            case "hot" -> Sort.by(
                    Sort.Order.desc("likeCount"),
                    Sort.Order.desc("viewCount")
            );

            case "views" -> Sort.by(
                    Sort.Order.desc("viewCount"),
                    Sort.Order.desc("likeCount")
            );

            default -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }

    private Specification<Content> buildSpecification(
            String category,
            String keyword
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (category != null
                    && !category.isBlank()
                    && !"全部".equals(category)
                    && !"all".equalsIgnoreCase(category)) {

                predicates.add(
                        cb.equal(root.get("category"), category.trim())
                );
            }

            if (keyword != null && !keyword.isBlank()) {

                String like = "%"
                        + keyword.trim().toLowerCase()
                        + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("title")), like),
                                cb.like(cb.lower(root.get("content")), like)
                        )
                );
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
