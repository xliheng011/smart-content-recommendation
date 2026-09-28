package com.example.backend.dto;

import java.util.List;

/**
 * 管理端概览数据。
 */
public class AdminOverviewResponse {

    private long contentCount;

    private long userCount;

    private long behaviorCount;

    private long totalViews;

    private long totalLikes;

    private long categoryCount;

    private List<CategoryStatResponse> categories;

    public AdminOverviewResponse() {
    }

    public long getContentCount() {
        return contentCount;
    }

    public void setContentCount(long contentCount) {
        this.contentCount = contentCount;
    }

    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    public long getBehaviorCount() {
        return behaviorCount;
    }

    public void setBehaviorCount(long behaviorCount) {
        this.behaviorCount = behaviorCount;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(long totalViews) {
        this.totalViews = totalViews;
    }

    public long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }

    public long getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(long categoryCount) {
        this.categoryCount = categoryCount;
    }

    public List<CategoryStatResponse> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryStatResponse> categories) {
        this.categories = categories;
    }
}
