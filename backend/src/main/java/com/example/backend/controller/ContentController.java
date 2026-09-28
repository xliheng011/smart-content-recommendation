package com.example.backend.controller;

import com.example.backend.common.ApiException;
import com.example.backend.common.ApiResponse;
import com.example.backend.common.PageResult;
import com.example.backend.dto.CategoryStatResponse;
import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.CreateContentRequest;
import com.example.backend.security.CurrentUserHolder;
import com.example.backend.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容读取与互动接口。
 */
@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * 内容列表：分页 + 分类 + 关键字 + 排序。
     *
     * GET /api/contents?page=1&size=12&category=科技&keyword=AI&sort=latest
     */
    @GetMapping
    public ApiResponse<PageResult<ContentResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort
    ) {

        return ApiResponse.ok(
                contentService.search(
                        category,
                        keyword,
                        sort,
                        page,
                        size,
                        CurrentUserHolder.getUserId()
                )
        );
    }

    /**
     * 分类聚合。
     *
     * GET /api/contents/categories
     */
    @GetMapping("/categories")
    public ApiResponse<List<CategoryStatResponse>> categories() {

        return ApiResponse.ok(contentService.getCategories());
    }

    /**
     * 我点赞过的内容。
     *
     * GET /api/contents/liked
     */
    @GetMapping("/liked")
    public ApiResponse<List<ContentResponse>> liked() {

        Long userId = requireUserId();

        return ApiResponse.ok(contentService.getLikedContents(userId));
    }

    /**
     * 我发布的内容。
     *
     * GET /api/contents/mine
     */
    @GetMapping("/mine")
    public ApiResponse<List<ContentResponse>> mine() {

        return ApiResponse.ok(
                contentService.getMyContents(requireUserId())
        );
    }

    /**
     * 我的阅读历史（按最近阅读时间倒序，已按内容去重）。
     *
     * GET /api/contents/history?limit=30
     */
    @GetMapping("/history")
    public ApiResponse<List<ContentResponse>> history(
            @RequestParam(defaultValue = "30") int limit
    ) {

        return ApiResponse.ok(
                contentService.getViewedContents(requireUserId(), limit)
        );
    }

    /**
     * 发布内容。登录用户即可发布，作者默认为自己。
     *
     * POST /api/contents
     */
    @PostMapping
    public ApiResponse<ContentResponse> create(
            @Valid @RequestBody CreateContentRequest request
    ) {

        return ApiResponse.ok(
                "发布成功",
                contentService.createContent(
                        request,
                        CurrentUserHolder.get()
                )
        );
    }

    /**
     * 内容详情（会累加浏览量并记录浏览行为）。
     *
     * GET /api/contents/1
     */
    @GetMapping("/{id}")
    public ApiResponse<ContentResponse> detail(@PathVariable Long id) {

        return ApiResponse.ok(
                contentService.getContentById(
                        id,
                        CurrentUserHolder.getUserId()
                )
        );
    }

    /**
     * 相关阅读。
     *
     * GET /api/contents/1/similar
     */
    @GetMapping("/{id}/similar")
    public ApiResponse<List<ContentResponse>> similar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit
    ) {

        return ApiResponse.ok(
                contentService.getSimilar(
                        id,
                        limit,
                        CurrentUserHolder.getUserId()
                )
        );
    }

    /**
     * 点赞（幂等）。
     *
     * POST /api/contents/1/like
     */
    @PostMapping("/{id}/like")
    public ApiResponse<ContentResponse> like(@PathVariable Long id) {

        return ApiResponse.ok(
                "点赞成功",
                contentService.likeContent(id, requireUserId())
        );
    }

    /**
     * 取消点赞（幂等）。
     *
     * DELETE /api/contents/1/like
     */
    @DeleteMapping("/{id}/like")
    public ApiResponse<ContentResponse> unlike(@PathVariable Long id) {

        return ApiResponse.ok(
                "已取消点赞",
                contentService.unlikeContent(id, requireUserId())
        );
    }

    /**
     * 删除内容。管理员可删任意内容，普通用户只能删自己发布的。
     *
     * DELETE /api/contents/1
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {

        contentService.deleteContent(id, CurrentUserHolder.get());

        return ApiResponse.ok("内容已删除", null);
    }

    private Long requireUserId() {

        Long userId = CurrentUserHolder.getUserId();

        if (userId == null) {
            throw ApiException.unauthorized("请先登录");
        }

        return userId;
    }
}
