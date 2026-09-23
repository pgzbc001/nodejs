package com.mdm.platform.duplicate.dto;

import java.util.Map;

/**
 * 查重请求：以草稿属性与同模型已有数据比对。
 */
public class DuplicateCheckRequest {

    private Long modelId;

    /** 待校验的动态属性（明文） */
    private Map<String, Object> attributes;

    /** 排除的已有数据 ID（编辑场景排除自身） */
    private Long excludeDataId;

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Long getExcludeDataId() {
        return excludeDataId;
    }

    public void setExcludeDataId(Long excludeDataId) {
        this.excludeDataId = excludeDataId;
    }
}
