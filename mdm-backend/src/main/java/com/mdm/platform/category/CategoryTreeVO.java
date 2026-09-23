package com.mdm.platform.category;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点：含子分类与关联模型数。
 */
public class CategoryTreeVO {

    private Long id;

    private String code;

    private String name;

    private Long parentId;

    private Integer sortNo;

    private String description;

    /** 该分类下未删除模型数 */
    private long modelCount;

    private List<CategoryTreeVO> children = new ArrayList<>();

    public CategoryTreeVO() {
    }

    public CategoryTreeVO(CategoryEntity entity, long modelCount) {
        this.id = entity.getId();
        this.code = entity.getCode();
        this.name = entity.getName();
        this.parentId = entity.getParentId();
        this.sortNo = entity.getSortNo();
        this.description = entity.getDescription();
        this.modelCount = modelCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getModelCount() {
        return modelCount;
    }

    public void setModelCount(long modelCount) {
        this.modelCount = modelCount;
    }

    public List<CategoryTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryTreeVO> children) {
        this.children = children;
    }
}
