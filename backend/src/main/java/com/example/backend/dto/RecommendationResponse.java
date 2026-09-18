package com.example.backend.dto;

public class RecommendationResponse {

    private Long id;
    private String title;
    private String category;
    private Integer score;
    private String content;
    private Integer viewCount;
    private Integer likeCount;

    public RecommendationResponse() {
    }

    public RecommendationResponse(
            Long id,
            String title,
            String category,
            Integer score,
            String content,
            Integer viewCount,
            Integer likeCount
    ) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.score = score;
        this.content = content;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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