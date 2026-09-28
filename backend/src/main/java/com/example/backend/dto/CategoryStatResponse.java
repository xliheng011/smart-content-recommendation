package com.example.backend.dto;

/**
 * 分类统计。
 */
public class CategoryStatResponse {

    private String name;

    private long count;

    public CategoryStatResponse() {
    }

    public CategoryStatResponse(String name, long count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
