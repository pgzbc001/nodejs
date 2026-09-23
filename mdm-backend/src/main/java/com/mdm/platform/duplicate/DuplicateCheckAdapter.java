package com.mdm.platform.duplicate;

/**
 * 相似度适配器（REQ-BKD07）：v1.0 本地算法实现，v2.0 可替换为 AI 服务。
 */
public interface DuplicateCheckAdapter {

    /**
     * 计算两个文本的相似度。
     *
     * @param a 文本一（可为 null/空白）
     * @param b 文本二（可为 null/空白）
     * @return 相似度分值 0~100（完全相同=100，完全不同=0）
     */
    double similarity(String a, String b);
}
