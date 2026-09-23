package com.mdm.platform.data.dto;

import com.mdm.platform.model.dto.FieldDef;

import java.util.List;

/**
 * 动态列表视图定义：前端按 listShow 渲染列、searchable 渲染筛选区。
 */
public class DataViewSchemaVO {

    private Long modelId;

    private String modelCode;

    private String modelName;

    /** 列表展示字段（listShow=true，含编码/名称前的固定列信息） */
    private List<FieldDef> listFields;

    /** 检索条件字段（searchable=true） */
    private List<FieldDef> searchFields;

    /** 全部字段（供表单/详情渲染） */
    private List<FieldDef> allFields;

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public List<FieldDef> getListFields() {
        return listFields;
    }

    public void setListFields(List<FieldDef> listFields) {
        this.listFields = listFields;
    }

    public List<FieldDef> getSearchFields() {
        return searchFields;
    }

    public void setSearchFields(List<FieldDef> searchFields) {
        this.searchFields = searchFields;
    }

    public List<FieldDef> getAllFields() {
        return allFields;
    }

    public void setAllFields(List<FieldDef> allFields) {
        this.allFields = allFields;
    }
}
