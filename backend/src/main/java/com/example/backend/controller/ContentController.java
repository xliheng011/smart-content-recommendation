package com.example.backend.controller;

import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.CreateContentRequest;
import com.example.backend.service.ContentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * 创建内容
     *
     * POST /api/contents
     */
    @PostMapping
    public ContentResponse createContent(
            @RequestBody CreateContentRequest request) {

        return contentService.createContent(request);
    }

    /**
     * 查询内容
     *
     * 示例：
     * GET /api/contents/1
     *
     * 带用户：
     * GET /api/contents/1?userId=1
     */
    @GetMapping("/{id}")
    public ContentResponse getContentById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {

        return contentService.getContentById(id, userId);
    }

    /**
     * 查询全部内容
     *
     * GET /api/contents
     */
    @GetMapping
    public List<ContentResponse> getAllContents() {

        return contentService.getAllContents();
    }

    /**
     * 点赞
     *
     * 示例：
     * POST /api/contents/1/like
     *
     * 带用户：
     * POST /api/contents/1/like?userId=1
     */
    @PostMapping("/{id}/like")
    public ContentResponse likeContent(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {

        return contentService.likeContent(id, userId);
    }


}