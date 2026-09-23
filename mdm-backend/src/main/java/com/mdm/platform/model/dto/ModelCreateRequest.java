package com.mdm.platform.model.dto;

import java.util.List;

/**
 * 模型创建请求：空白创建或继承既有模型（复制字段/编码规则/扩展配置）。
 */
public class ModelCreateRequest {

    private String code;

    private String name;

    private Long categoryId;

    private String dept;

    private String description;

    /** 创建模式：BLANK 空白 / INHERIT 继承（inheritFromId 必填） */
    private String mode;

    /** 继承来源模型 ID（mode=INHERIT 时复制其 fieldDefs/codeRules/extConfig） */
    private Long inheritFromId;

    private List<FieldDef> fieldDefs;

    private List<CodeRuleSegment> codeRules;

    private ExtConfig extConfig;

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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Long getInheritFromId() {
        return inheritFromId;
    }

    public void setInheritFromId(Long inheritFromId) {
        this.inheritFromId = inheritFromId;
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
