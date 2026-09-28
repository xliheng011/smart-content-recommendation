package com.example.backend.service;

import com.example.backend.common.ApiException;
import com.example.backend.dto.CategoryStatResponse;
import com.example.backend.dto.CreateUserRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.dto.UserStatsResponse;
import com.example.backend.entity.Content;
import com.example.backend.entity.User;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserBehaviorRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final int FAVORITE_LIMIT = 5;

    /** 个人中心"最近动态"展示条数 */
    private static final int RECENT_BEHAVIOR_LIMIT = 10;

    private final UserRepository userRepository;

    private final UserBehaviorRepository userBehaviorRepository;

    private final ContentRepository contentRepository;

    private final UserBehaviorService userBehaviorService;

    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserBehaviorRepository userBehaviorRepository,
            ContentRepository contentRepository,
            UserBehaviorService userBehaviorService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userBehaviorRepository = userBehaviorRepository;
        this.contentRepository = contentRepository;
        this.userBehaviorService = userBehaviorService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 创建用户（管理员新增账号时使用，默认角色 USER）。
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        String username = request.getUsername() == null
                ? ""
                : request.getUsername().trim();

        if (username.isBlank()) {
            throw ApiException.badRequest("用户名不能为空");
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw ApiException.badRequest("密码不能为空");
        }

        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("该用户名已被注册");
        }

        User user = new User();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(
                request.getEmail() == null || request.getEmail().isBlank()
                        ? null
                        : request.getEmail().trim()
        );
        user.setNickname(
                request.getNickname() == null || request.getNickname().isBlank()
                        ? username
                        : request.getNickname().trim()
        );
        user.setAvatarUrl(request.getAvatarUrl());
        user.setRole("USER");

        LocalDateTime now = LocalDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        return toResponse(
                userRepository.findById(id)
                        .orElseThrow(() ->
                                ApiException.notFound("用户不存在")
                        )
        );
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 个人中心统计：阅读数、点赞数、兴趣画像、最近行为。
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));

        UserStatsResponse stats = new UserStatsResponse();

        stats.setUserId(user.getId());
        stats.setUsername(user.getUsername());
        stats.setNickname(user.getNickname());
        stats.setAvatarUrl(user.getAvatarUrl());
        stats.setRole(user.getRole());

        stats.setViewedCount(
                userBehaviorService.countDistinctContent(userId, "VIEW")
        );

        stats.setLikedCount(
                userBehaviorService.countDistinctContent(userId, "LIKE")
        );

        stats.setBehaviorCount(
                userBehaviorService.countByUser(userId)
        );

        stats.setFavoriteCategories(buildFavoriteCategories(userId));

        stats.setRecentBehaviors(
                userBehaviorService.getRecentBehaviors(
                        userId,
                        RECENT_BEHAVIOR_LIMIT
                )
        );

        return stats;
    }

    /**
     * 兴趣画像：按分类累计兴趣分（点赞 3 分、浏览 1 分）后取前几名。
     */
    private List<CategoryStatResponse> buildFavoriteCategories(Long userId) {

        List<UserBehavior> behaviors =
                userBehaviorRepository.findByUserId(userId);

        if (behaviors.isEmpty()) {
            return List.of();
        }

        Map<Long, Content> contentIndex = new HashMap<>();

        contentRepository.findAllById(
                behaviors.stream()
                        .map(UserBehavior::getContentId)
                        .toList()
        ).forEach(content ->
                contentIndex.put(content.getId(), content)
        );

        Map<String, Long> scores = new HashMap<>();

        for (UserBehavior behavior : behaviors) {

            Content content = contentIndex.get(behavior.getContentId());

            if (content == null
                    || content.getCategory() == null
                    || content.getCategory().isBlank()) {
                continue;
            }

            long weight = ContentMapper.BEHAVIOR_LIKE
                    .equals(behavior.getBehaviorType()) ? 3L : 1L;

            scores.merge(content.getCategory(), weight, Long::sum);
        }

        List<CategoryStatResponse> result = new ArrayList<>();

        scores.forEach((name, score) ->
                result.add(new CategoryStatResponse(name, score))
        );

        result.sort(
                Comparator.comparingLong(CategoryStatResponse::getCount)
                        .reversed()
        );

        return result.stream()
                .limit(FAVORITE_LIMIT)
                .toList();
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }
}
