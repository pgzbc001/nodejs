package com.mdm.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 模型版本快照实体。
 */
@Entity
@Table(name = "mdm_model_version")
public class ModelVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /** 快照时刻的模型状态 */
    @Column(name = "status")
    private String status;

    /** 模型完整定义 JSON */
    @Column(name = "snapshot", nullable = false)
    private String snapshot;

    /** CREATE / UPDATE / ONLINE / OFFLINE / ROLLBACK */
    @Column(name = "operation")
    private String operation;

    @Column(name = "operator")
    private String operator;

    @Column(name = "operated_time")
    private String operatedTime;

    @Column(name = "del_flag", nullable = false)
    private Integer delFlag = 0;

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

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
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

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }
}
