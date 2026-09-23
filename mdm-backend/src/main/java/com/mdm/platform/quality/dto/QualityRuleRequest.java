package com.mdm.platform.quality.dto;

/**
 * 质量规则创建/修改请求。
 */
public class QualityRuleRequest {

    private Long modelId;

    /** COMPLIANCE / CONSISTENCY / COMPLETENESS */
    private String ruleType;

    private String fieldName;

    /** 表达式 JSON 对象（结构由 rule_type 决定） */
    private Object expression;

    /** CRITICAL / WARNING / INFO */
    private String severity;

    private String message;

    private Boolean enabled;

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getExpression() {
        return expression;
    }

    public void setExpression(Object expression) {
        this.expression = expression;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
