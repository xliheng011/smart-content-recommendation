package com.example.backend.service;

import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.CreateContentRequest;
import com.example.backend.entity.Content;
import com.example.backend.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    public ContentResponse getContentById(Long id) {

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
}