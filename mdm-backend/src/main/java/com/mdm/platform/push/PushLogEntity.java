package com.mdm.platform.push;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 数据推送日志实体。
 */
@Entity
@Table(name = "mdm_push_log")
public class PushLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_id", nullable = false)
    private Long dataId;

    @Column(name = "data_code")
    private String dataCode;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "system_id", nullable = false)
    private Long systemId;

    @Column(name = "system_code")
    private String systemCode;

    /** SUCCESS / FAIL */
    @Column(name = "result", nullable = false)
    private String result;

    @Column(name = "detail")
    private String detail;

    @Column(name = "operator")
    private String operator;

    @Column(name = "pushed_time")
    private String pushedTime;

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

    public String getDataCode() {
        return dataCode;
    }

    public void setDataCode(String dataCode) {
        this.dataCode = dataCode;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getSystemId() {
        return systemId;
    }

    public void setSystemId(Long systemId) {
        this.systemId = systemId;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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

    public String getPushedTime() {
        return pushedTime;
    }

    public void setPushedTime(String pushedTime) {
        this.pushedTime = pushedTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }
}
