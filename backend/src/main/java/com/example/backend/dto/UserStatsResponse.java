package com.example.backend.dto;

import java.util.List;

/**
 * 个人中心统计数据。
 */
public class UserStatsResponse {

    private Long userId;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String role;

    /** 阅读过的内容去重数量 */
    private long viewedCount;

    /** 点赞过的内容去重数量 */
    private long likedCount;

    /** 行为总条数 */
    private long behaviorCount;

    /** 兴趣画像：按兴趣分排序的分类 */
    private List<CategoryStatResponse> favoriteCategories;

    /** 最近行为 */
    private List<BehaviorResponse> recentBehaviors;

    public UserStatsResponse() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getViewedCount() {
        return viewedCount;
    }

    public void setViewedCount(long viewedCount) {
        this.viewedCount = viewedCount;
    }

    public long getLikedCount() {
        return likedCount;
    }

    public void setLikedCount(long likedCount) {
        this.likedCount = likedCount;
    }

    public long getBehaviorCount() {
        return behaviorCount;
    }

    public void setBehaviorCount(long behaviorCount) {
        this.behaviorCount = behaviorCount;
    }

    public List<CategoryStatResponse> getFavoriteCategories() {
        return favoriteCategories;
    }

    public void setFavoriteCategories(
            List<CategoryStatResponse> favoriteCategories
    ) {
        this.favoriteCategories = favoriteCategories;
    }

    public List<BehaviorResponse> getRecentBehaviors() {
        return recentBehaviors;
    }

    public void setRecentBehaviors(List<BehaviorResponse> recentBehaviors) {
        this.recentBehaviors = recentBehaviors;
    }
}
