package com.mdm.platform.model.dto;

import java.util.List;

/**
 * 模型详情视图：JSON 列解析为结构化对象。
 */
public class ModelVO {

    private Long id;

    private String code;

    private String name;

    private Long categoryId;

    /** 分类名称（冗余展示） */
    private String categoryName;

    private String dept;

    /** ONLINE / OFFLINE */
    private String status;

    private Integer versionNo;

    private List<FieldDef> fieldDefs;

    private List<CodeRuleSegment> codeRules;

    private ExtConfig extConfig;

    private String description;

    private String createdBy;

    private String createdTime;

    private String updatedBy;

    private String updatedTime;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public List<FieldDef> getFieldDefs() {
        return fieldDefs;
    }

    public void setFieldDefs(List<FieldDef> fieldDefs) {
        this.fieldDefs = fieldDefs;
    }

    public List<CodeRuleSegment> getCodeRules() {
        return codeRules;
    }

    public void setCodeRules(List<CodeRuleSegment> codeRules) {
        this.codeRules = codeRules;
    }

    public ExtConfig getExtConfig() {
        return extConfig;
    }

    public void setExtConfig(ExtConfig extConfig) {
        this.extConfig = extConfig;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }
}
