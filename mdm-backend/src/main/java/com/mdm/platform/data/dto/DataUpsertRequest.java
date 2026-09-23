package com.mdm.platform.data.dto;

import java.util.List;
import java.util.Map;

/**
 * 主数据新增/修改请求：动态属性 + 可选手填编码 + 忽略的告警规则。
 */
public class DataUpsertRequest {

    /** 明文动态属性 */
    private Map<String, Object> attributes;

    /** 手填编码（模型无编码规则或需指定时；缺省自动生成） */
    private String code;

    /** 提交时确认忽略的告警规则 ID（仅 WARNING/INFO 有效） */
    private List<Long> ignoredWarnings;

    /** 忽略原因 */
    private String ignoreReason;

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<Long> getIgnoredWarnings() {
        return ignoredWarnings;
    }

    public void setIgnoredWarnings(List<Long> ignoredWarnings) {
        this.ignoredWarnings = ignoredWarnings;
    }

    public String getIgnoreReason() {
        return ignoreReason;
    }

    public void setIgnoreReason(String ignoreReason) {
        this.ignoreReason = ignoreReason;
    }
}
