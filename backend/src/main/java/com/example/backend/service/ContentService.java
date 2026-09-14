package com.example.backend.service;

import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.CreateContentRequest;
import com.example.backend.entity.Content;
import com.example.backend.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public ContentResponse createContent(CreateContentRequest request) {

        Content content = new Content();

        content.setTitle(request.getTitle());
        content.setContent(request.getContent());
        content.setCategory(request.getCategory());
        content.setAuthorId(request.getAuthorId());

        content.setViewCount(0);
        content.setLikeCount(0);

        LocalDateTime now = LocalDateTime.now();
        content.setCreatedAt(now);
        content.setUpdatedAt(now);

        Content savedContent = contentRepository.save(content);

        return new ContentResponse(
                savedContent.getId(),
                savedContent.getTitle(),
                savedContent.getContent(),
                savedContent.getCategory(),
                savedContent.getAuthorId(),
                savedContent.getViewCount(),
                savedContent.getLikeCount()
        );
    }
    @Transactional
    public ContentResponse getContentById(Long id) {

        // 1. 查询内容是否存在
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("内容不存在"));

        // 2. 使用数据库原子操作增加浏览量
        contentRepository.incrementViewCount(id);

        // 3. 重新查询，拿到最新的浏览量
        Content updatedContent = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("内容不存在"));

        // 4. 返回最新内容
        return new ContentResponse(
                updatedContent.getId(),
                updatedContent.getTitle(),
                updatedContent.getContent(),
                updatedContent.getCategory(),
                updatedContent.getAuthorId(),
                updatedContent.getViewCount(),
                updatedContent.getLikeCount()
        );
    }
    public List<ContentResponse> getAllContents() {

        return contentRepository.findAll()
                .stream()
                .map(content -> new ContentResponse(
                        content.getId(),
                        content.getTitle(),
                        content.getContent(),
                        content.getCategory(),
                        content.getAuthorId(),
                        content.getViewCount(),
                        content.getLikeCount()
                ))
                .toList();
    }

    @Transactional
    public ContentResponse likeContent(Long id) {

        int updatedRows = contentRepository.incrementLikeCount(id);

        if (updatedRows == 0) {
            throw new RuntimeException("内容不存在");
        }

        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("内容不存在"));

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