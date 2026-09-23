package com.mdm.platform.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 模型扩展配置（见需求 4.3.3）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtConfig {

    /** 流程展示标题取自哪个字段 */
    private String titleField;

    /** 数据本身是否为树形结构 */
    private boolean treeEnabled;

    /** 树形结构：上级节点字段 */
    private String parentField;

    /** 树形结构：子级节点字段 */
    private String childField;

    /** 树形结构：展示字段 */
    private String displayField;

    public String getTitleField() {
        return titleField;
    }

    public void setTitleField(String titleField) {
        this.titleField = titleField;
    }

    public boolean isTreeEnabled() {
        return treeEnabled;
    }

    public void setTreeEnabled(boolean treeEnabled) {
        this.treeEnabled = treeEnabled;
    }

    public String getParentField() {
        return parentField;
    }

    public void setParentField(String parentField) {
        this.parentField = parentField;
    }

    public String getChildField() {
        return childField;
    }

    public void setChildField(String childField) {
        this.childField = childField;
    }

    public String getDisplayField() {
        return displayField;
    }

    public void setDisplayField(String displayField) {
        this.displayField = displayField;
    }
}
