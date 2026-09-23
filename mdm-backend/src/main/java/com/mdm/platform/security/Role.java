package com.mdm.platform.security;

/**
 * 平台角色（对应 PRD 用户角色）。
 */
public enum Role {

    /** 数据管理员：模型定义与维护 */
    MODEL_ADMIN,
    /** 数据录入员：主数据录入维护 */
    DATA_STAFF,
    /** 数据审核员：协同确认与推送 */
    DATA_AUDITOR,
    /** 系统管理员：全量权限 */
    SYS_ADMIN
}
