package com.tns.mes.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

public class PageResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResponse() { }

    public PageResponse(List<T> items, long total, int page, int size, int totalPages) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
    }

    public static <E> PageResponse<E> from(Page<E> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getNumber(),
                page.getSize(), page.getTotalPages());
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}

