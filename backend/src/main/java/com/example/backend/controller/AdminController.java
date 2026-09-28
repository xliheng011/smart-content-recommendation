package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.AdminOverviewResponse;
import com.example.backend.dto.CategoryStatResponse;
import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.CreateContentRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.entity.Content;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserBehaviorRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.CurrentUserHolder;
import com.example.backend.service.ContentService;
import com.example.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端接口。
 *
 * 整个 /api/admin/** 前缀由 AuthInterceptor 强制校验管理员身份，
 * 这里不需要重复写权限判断。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ContentService contentService;

    private final UserService userService;

    private final ContentRepository contentRepository;

    private final UserRepository userRepository;

    private final UserBehaviorRepository userBehaviorRepository;

    public AdminController(
            ContentService contentService,
            UserService userService,
            ContentRepository contentRepository,
            UserRepository userRepository,
            UserBehaviorRepository userBehaviorRepository
    ) {
        this.contentService = contentService;
        this.userService = userService;
        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.userBehaviorRepository = userBehaviorRepository;
    }

    /**
     * 发布内容（管理员可以指定 authorId 代他人发布）。
     *
     * POST /api/admin/contents
     */
    @PostMapping("/contents")
    public ApiResponse<ContentResponse> createContent(
            @Valid @RequestBody CreateContentRequest request
    ) {

        return ApiResponse.ok(
                "内容发布成功",
                contentService.createContent(
                        request,
                        CurrentUserHolder.get()
                )
        );
    }

    /**
     * 用户列表。
     *
     * GET /api/admin/users
     */
    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> users() {

        return ApiResponse.ok(userService.getAllUsers());
    }

    /**
     * 数据概览。
     *
     * GET /api/admin/overview
     */
    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> overview() {

        AdminOverviewResponse overview = new AdminOverviewResponse();

        List<Content> contents = contentRepository.findAll();

        overview.setContentCount(contents.size());
        overview.setUserCount(userRepository.count());
        overview.setBehaviorCount(userBehaviorRepository.count());

        long totalViews = 0;
        long totalLikes = 0;

        for (Content content : contents) {

            totalViews += content.getViewCount() == null
                    ? 0
                    : content.getViewCount();

            totalLikes += content.getLikeCount() == null
                    ? 0
                    : content.getLikeCount();
        }

        overview.setTotalViews(totalViews);
        overview.setTotalLikes(totalLikes);

        List<CategoryStatResponse> categories = contentService.getCategories();

        overview.setCategories(categories);
        overview.setCategoryCount(categories.size());

        return ApiResponse.ok(overview);
    }

    /**
     * 删除内容（管理员可删任意内容，同时清理关联行为）。
     *
     * DELETE /api/admin/contents/{id}
     */
    @DeleteMapping("/contents/{id}")
    public ApiResponse<Void> deleteContent(@PathVariable Long id) {

        contentService.deleteContent(id, CurrentUserHolder.get());

        return ApiResponse.ok("内容已删除", null);
    }
}
