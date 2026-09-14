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

    @PostMapping
    public ContentResponse createContent(
            @RequestBody CreateContentRequest request) {

        return contentService.createContent(request);
    }

    @GetMapping("/{id}")
    public ContentResponse getContentById(@PathVariable Long id) {
        return contentService.getContentById(id);
    }

    @GetMapping
    public List<ContentResponse> getAllContents() {
        return contentService.getAllContents();
    }

    @PostMapping("/{id}/like")
    public ContentResponse likeContent(@PathVariable Long id) {
        return contentService.likeContent(id);
    }
}