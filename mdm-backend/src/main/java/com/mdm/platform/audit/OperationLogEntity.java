package com.mdm.platform.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 操作日志实体：全操作留痕（REQ-BKD10）。
 */
@Entity
@Table(name = "mdm_operation_log")
public class OperationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务类型：CATEGORY / MODEL / DATA / PUSH / COLLABORATION / QUALITY / IMPORT_EXPORT */
    @Column(name = "biz_type", nullable = false)
    private String bizType;

    /** 操作：CREATE / UPDATE / DELETE / ONLINE / OFFLINE / DISABLE / ENABLE / ROLLBACK / PUSH / CONFIRM / REJECT / IMPORT / EXPORT ... */
    @Column(name = "operation", nullable = false)
    private String operation;

    /** 目标对象 ID */
    @Column(name = "target_id")
    private String targetId;

    /** 目标描述（如模型名称/数据编码） */
    @Column(name = "target_desc")
    private String targetDesc;

    /** 详情 JSON */
    @Column(name = "detail")
    private String detail;

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

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetDesc() {
        return targetDesc;
    }

    public void setTargetDesc(String targetDesc) {
        this.targetDesc = targetDesc;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
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
