package com.example.backend.service;

import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.CreateContentRequest;
import com.example.backend.entity.Content;
import com.example.backend.repository.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserBehaviorService userBehaviorService;

    public ContentService(
            ContentRepository contentRepository,
            UserBehaviorService userBehaviorService) {

        this.contentRepository = contentRepository;
        this.userBehaviorService = userBehaviorService;
    }

    /**
     * 创建内容
     */
    public ContentResponse createContent(CreateContentRequest request) {

        Content content = new Content();

        content.setTitle(request.getTitle());
        content.setContent(request.getContent());
        content.setCategory(request.getCategory());
        content.setAuthorId(request.getAuthorId());

        // 初始浏览量和点赞量
        content.setViewCount(0);
        content.setLikeCount(0);

        // 创建时间
        LocalDateTime now = LocalDateTime.now();
        content.setCreatedAt(now);
        content.setUpdatedAt(now);

        // 保存到数据库
        Content savedContent = contentRepository.save(content);

        return toResponse(savedContent);
    }

    /**
     * 查询内容
     *
     * userId 不为空时：
     * 1. 浏览量 +1
     * 2. 记录用户 VIEW 行为
     */
    @Transactional
    public ContentResponse getContentById(Long id, Long userId) {

        // 1. 查询内容
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("内容不存在"));

        // 2. 浏览量 +1
        contentRepository.incrementViewCount(id);

        // 3. 如果用户已登录，记录 VIEW 行为
        if (userId != null) {
            userBehaviorService.recordBehavior(
                    userId,
                    id,
                    "VIEW"
            );
        }

        // 4. 重新查询最新数据
        Content updatedContent = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("内容不存在"));

        // 5. 返回
        return toResponse(updatedContent);
    }

    /**
     * 查询全部内容
     */
    public List<ContentResponse> getAllContents() {

        return contentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 点赞内容
     *
     * userId 不为空时：
     * 1. 点赞数 +1
     * 2. 记录用户 LIKE 行为
     */
    @Transactional
    public ContentResponse likeContent(Long id, Long userId) {

        // 1. 点赞数 +1
        int updatedRows = contentRepository.incrementLikeCount(id);

        // 2. 内容不存在
        if (updatedRows == 0) {
            throw new RuntimeException("内容不存在");
        }

        // 3. 如果用户已登录，记录 LIKE 行为
        if (userId != null) {
            userBehaviorService.recordBehavior(
                    userId,
                    id,
                    "LIKE"
            );
        }

        // 4. 查询最新内容
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("内容不存在"));

        // 5. 返回
        return toResponse(content);
    }

    /**
     * Entity -> Response
     */
    private ContentResponse toResponse(Content content) {

        return new ContentResponse(
                content.getId(),
                content.getTitle(),
                content.getContent(),
                content.getCategory(),
                content.getAuthorId(),
                content.getViewCount(),
                content.getLikeCount()
        );
    }
}