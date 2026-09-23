package com.mdm.platform.quality.dto;

/**
 * 单条质量违规结果。
 */
public class QualityViolationVO {

    private Long ruleId;

    private String fieldName;

    /** CRITICAL / WARNING / INFO */
    private String severity;

    private String message;

    public QualityViolationVO() {
    }

    public QualityViolationVO(Long ruleId, String fieldName, String severity, String message) {
        this.ruleId = ruleId;
        this.fieldName = fieldName;
        this.severity = severity;
        this.message = message;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
