package com.mdm.platform.duplicate.dto;

import java.util.List;

/**
 * 查重结果：Top 5 相似数据与整体判定。
 */
public class DuplicateCheckResultVO {

    /** 全部记录中的最高相似度（无相似数据时为 0） */
    private double maxSimilarity;

    /** 是否存在高度相似（≥80）数据 */
    private boolean hasHighSimilar;

    /** 相似度 ≥60 的前 5 条 */
    private List<SimilarItemVO> items;

    public double getMaxSimilarity() {
        return maxSimilarity;
    }

    public void setMaxSimilarity(double maxSimilarity) {
        this.maxSimilarity = maxSimilarity;
    }

    public boolean isHasHighSimilar() {
        return hasHighSimilar;
    }

    public void setHasHighSimilar(boolean hasHighSimilar) {
        this.hasHighSimilar = hasHighSimilar;
    }

    public List<SimilarItemVO> getItems() {
        return items;
    }

    public void setItems(List<SimilarItemVO> items) {
        this.items = items;
    }
}
