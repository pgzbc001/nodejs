package com.mdm.platform.model.dto;

/**
 * 模型版本条目视图。
 */
public class ModelVersionVO {

    private Long id;

    private Long modelId;

    private Integer versionNo;

    /** 快照时刻模型状态 */
    private String status;

    /** CREATE / UPDATE / ONLINE / OFFLINE / ROLLBACK */
    private String operation;

    private String operator;

    private String operatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperatedTime() {
        return operatedTime;
    }

    public void setOperatedTime(String operatedTime) {
        this.operatedTime = operatedTime;
    }
}
