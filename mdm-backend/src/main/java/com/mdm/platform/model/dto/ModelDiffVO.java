package com.mdm.platform.model.dto;

import java.util.List;

/**
 * 模型版本差异视图：基础信息/字段/编码规则/扩展配置逐项对比。
 */
public class ModelDiffVO {

    private Integer fromVersion;

    private Integer toVersion;

    private List<ItemChange> changes;

    /**
     * 单项变更。
     * type：BASE 基础信息 / FIELD_ADD 新增字段 / FIELD_REMOVE 删除字段 / FIELD_MODIFY 字段属性修改 /
     * CODE_RULE 编码规则 / EXT_CONFIG 扩展配置
     */
    public static class ItemChange {

        private String type;

        /** 变更对象（基础属性名 / 字段名 / 字段名.属性名） */
        private String field;

        private String oldValue;

        private String newValue;

        public ItemChange() {
        }

        public ItemChange(String type, String field, String oldValue, String newValue) {
            this.type = type;
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }
    }

    public Integer getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(Integer fromVersion) {
        this.fromVersion = fromVersion;
    }

    public Integer getToVersion() {
        return toVersion;
    }

    public void setToVersion(Integer toVersion) {
        this.toVersion = toVersion;
    }

    public List<ItemChange> getChanges() {
        return changes;
    }

    public void setChanges(List<ItemChange> changes) {
        this.changes = changes;
    }
}
