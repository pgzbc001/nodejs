package com.mdm.platform.common;

/**
 * 统一错误码（见 api-mdm-0001.md 二、错误码定义）。
 */
public enum ErrorCode {

    SUCCESS(0, "success"),

    BAD_REQUEST(40000, "参数校验失败"),
    UNAUTHENTICATED(40101, "未识别操作人，请携带 X-User-Id/X-User-Name/X-User-Role 请求头"),
    FORBIDDEN(40300, "当前角色无权执行该操作"),
    NOT_FOUND(40400, "资源不存在"),

    CATEGORY_HAS_CHILDREN(40901, "分类下存在子分类或已关联数据模型，不允许删除"),
    MODEL_CODE_DUPLICATED(40902, "模型编码已存在"),
    MODEL_STRUCTURE_LOCKED(40903, "模型已上线，字段结构已锁定，仅允许新增字段或修改展示类属性"),
    MODEL_NOT_ONLINE(40904, "模型未上线，不允许录入数据"),
    DATA_CODE_DUPLICATED(40905, "数据编码已存在"),
    FIELD_VALUE_DUPLICATED(40906, "唯一字段值重复"),
    QUALITY_CRITICAL_BLOCKED(40907, "存在严重级质量问题，必须修正后才能提交"),
    COLLAB_PENDING(40908, "数据处于待协同确认状态，禁止推送"),
    DOWNSTREAM_CHECK_FAILED(40909, "下游系统校验未通过，未获强制执行确认"),
    CODE_RULE_INVALID(40910, "编码规则非法：编码引用类型只能出现在第一段"),

    CODE_GENERATE_FAILED(42200, "编码生成失败：唯一性冲突重试超限"),
    IMPORT_FILE_INVALID(42201, "导入文件解析失败"),

    SYSTEM_ERROR(50000, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
