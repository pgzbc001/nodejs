package com.mdm.platform.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 主数据版本快照实体。
 */
@Entity
@Table(name = "mdm_data_version")
public class DataVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_id", nullable = false)
    private Long dataId;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "code")
    private String code;

    /** 属性完整 JSON 快照（含加密密文） */
    @Column(name = "snapshot", nullable = false)
    private String snapshot;

    /** CREATE / UPDATE / DISABLE / ENABLE / ROLLBACK / DELETE */
    @Column(name = "operation")
    private String operation;

    @Column(name = "operator")
    private String operator;

    @Column(name = "operated_time")
    private String operatedTime;

    @Column(name = "remark")
    private String remark;

    @Column(name = "del_flag", nullable = false)
    private Integer delFlag = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDataId() {
        return dataId;
    }

    public void setDataId(Long dataId) {
        this.dataId = dataId;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }
}
