package com.mdm.platform.duplicate.dto;

import java.util.Map;

/**
 * 相似数据条目。
 */
public class SimilarItemVO {

    private Long dataId;

    private String code;

    private String name;

    /** 相似度 0~100 */
    private double similarity;

    /** 是否高度相似（≥80） */
    private boolean highSimilar;

    /** 明文属性（供前端回显对比） */
    private Map<String, Object> attributes;

    public Long getDataId() {
        return dataId;
    }

    public void setDataId(Long dataId) {
        this.dataId = dataId;
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

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public boolean isHighSimilar() {
        return highSimilar;
    }

    public void setHighSimilar(boolean highSimilar) {
        this.highSimilar = highSimilar;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
