package com.mdm.platform.data.dto;

import java.util.Map;

/**
 * 主数据列表行视图：动态属性（脱敏后）。
 */
public class DataViewVO {

    private Long id;

    private Long modelId;

    private String code;

    private String name;

    /** VALID / DISABLED / DRAFT */
    private String status;

    /** PENDING_CONFIRM / CONFIRMED / REJECTED / null */
    private String collabStatus;

    private Integer versionNo;

    private String updatedBy;

    private String updatedTime;

    /** 明文/脱敏后的动态属性（仅 listShow 字段由前端裁剪展示） */
    private Map<String, Object> attributes;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCollabStatus() {
        return collabStatus;
    }

    public void setCollabStatus(String collabStatus) {
        this.collabStatus = collabStatus;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
