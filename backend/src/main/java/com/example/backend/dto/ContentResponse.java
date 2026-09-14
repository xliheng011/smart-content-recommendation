package com.example.backend.dto;

public class ContentResponse {

    private Long id;
    private String title;
    private String content;
    private String category;
    private Long authorId;
    private Integer viewCount;
    private Integer likeCount;

    public ContentResponse() {
    }

    public ContentResponse(
            Long id,
            String title,
            String content,
            String category,
            Long authorId,
            Integer viewCount,
            Integer likeCount) {

        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.authorId = authorId;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }
}