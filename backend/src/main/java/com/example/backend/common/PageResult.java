package com.example.backend.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 分页结果包装。
 *
 * page 从 1 开始，对前端更友好。
 */
public class PageResult<T> {

    private List<T> items;

    private long total;

    private int page;

    private int size;

    private int totalPages;

    private boolean hasNext;

    private boolean hasPrevious;

    public PageResult() {
    }

    public PageResult(
            List<T> items,
            long total,
            int page,
            int size
    ) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        this.hasNext = page < this.totalPages;
        this.hasPrevious = page > 1;
    }

    /**
     * 由 Spring Data 的 Page 转换而来。
     */
    public static <E, T> PageResult<T> of(
            Page<E> source,
            Function<E, T> mapper
    ) {
        List<T> items = source.getContent()
                .stream()
                .map(mapper)
                .toList();

        return new PageResult<>(
                items,
                source.getTotalElements(),
                source.getNumber() + 1,
                source.getSize()
        );
    }

    public static <T> PageResult<T> of(
            List<T> items,
            long total,
            int page,
            int size
    ) {
        return new PageResult<>(items, total, page, size);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0, page, size);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
