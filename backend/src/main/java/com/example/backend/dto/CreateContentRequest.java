package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建内容请求（管理员）。
 */
public class CreateContentRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    @NotBlank(message = "正文不能为空")
    private String content;

    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类长度不能超过 50 个字符")
    private String category;

    /** 不传时默认使用当前登录用户 */
    private Long authorId;

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
}
