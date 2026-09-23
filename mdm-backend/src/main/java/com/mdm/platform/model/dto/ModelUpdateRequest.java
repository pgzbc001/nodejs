package com.mdm.platform.model.dto;

import java.util.List;

/**
 * 模型更新请求：编码不可变更；上线模型仅展示类属性可改（结构锁定 40903）。
 */
public class ModelUpdateRequest {

    private String name;

    private Long categoryId;

    private String dept;

    private String description;

    private List<FieldDef> fieldDefs;

    private List<CodeRuleSegment> codeRules;

    private ExtConfig extConfig;

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

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
