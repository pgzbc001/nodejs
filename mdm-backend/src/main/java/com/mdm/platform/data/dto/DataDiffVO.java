package com.mdm.platform.data.dto;

import java.util.List;

/**
 * 主数据版本差异视图：逐字段新旧值对比。
 */
public class DataDiffVO {

    private Integer fromVersion;

    private Integer toVersion;

    private List<FieldChange> changes;

    /**
     * 单字段变更。
     */
    public static class FieldChange {

        private String field;

        private String label;

        private String oldValue;

        private String newValue;

        public FieldChange() {
        }

        public FieldChange(String field, String label, String oldValue, String newValue) {
            this.field = field;
            this.label = label;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
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

    public List<FieldChange> getChanges() {
        return changes;
    }

    public void setChanges(List<FieldChange> changes) {
        this.changes = changes;
    }
}
