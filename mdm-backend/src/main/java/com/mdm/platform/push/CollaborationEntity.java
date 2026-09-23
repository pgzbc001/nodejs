package com.mdm.platform.push;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 协同确认单实体：敏感字段变更触发，审核员确认/驳回。
 */
@Entity
@Table(name = "mdm_collaboration")
public class CollaborationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_id", nullable = false)
    private Long dataId;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    /** 发起原因（如：绝密字段"联系人电话"发生变更） */
    @Column(name = "reason")
    private String reason;

    /** 协作方（审核员） */
    @Column(name = "collaborator")
    private String collaborator;

    /** PENDING / CONFIRMED / REJECTED */
    @Column(name = "status")
    private String status = "PENDING";

    /** 申请人（发起修改者） */
    @Column(name = "applicant")
    private String applicant;

    @Column(name = "apply_time")
    private String applyTime;

    @Column(name = "confirm_time")
    private String confirmTime;

    @Column(name = "confirm_comment")
    private String confirmComment;

    @Column(name = "del_flag", nullable = false)
    private Integer delFlag = 0;

    public boolean isPending() {
        return "PENDING".equals(status);
    }

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCollaborator() {
        return collaborator;
    }

    public void setCollaborator(String collaborator) {
        this.collaborator = collaborator;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public String getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(String applyTime) {
        this.applyTime = applyTime;
    }

    public String getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(String confirmTime) {
        this.confirmTime = confirmTime;
    }

    public String getConfirmComment() {
        return confirmComment;
    }

    public void setConfirmComment(String confirmComment) {
        this.confirmComment = confirmComment;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }
}
