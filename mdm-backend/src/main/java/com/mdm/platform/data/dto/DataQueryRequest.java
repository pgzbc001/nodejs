package com.mdm.platform.data.dto;

import java.util.Map;

/**
 * 动态数据查询请求：关键字（编码/名称）+ 状态 + searchable 字段动态筛选。
 */
public class DataQueryRequest {

    private Long modelId;

    /** 编码/名称模糊匹配 */
    private String keyword;

    /** VALID / DISABLED / DRAFT / 空=全部 */
    private String status;

    /** searchable 字段的等值/模糊筛选（前端由动态筛选区生成） */
    private Map<String, String> filters;

    private int page = 1;

    private int size = 20;

    /** 排序字段（字段名或 code/name/updatedTime） */
    private String sortField;

    /** asc / desc */
    private String sortOrder = "desc";

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
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

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
