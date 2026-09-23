package com.mdm.platform.duplicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地相似度算法单元测试（tasks.md T16）：完全相同=100、完全不同=0、归一化编辑距离、2-gram Jaccard。
 */
class LocalSimilarityAdapterTest {

    private final LocalSimilarityAdapter adapter = new LocalSimilarityAdapter();

    @Test
    void identicalTextShouldScore100() {
        assertEquals(100, adapter.similarity("不锈钢板304", "不锈钢板304"));
    }

    @Test
    void blankShouldScore0() {
        assertEquals(0, adapter.similarity(null, "abc"));
        assertEquals(0, adapter.similarity("", "abc"));
        assertEquals(0, adapter.similarity("abc", null));
    }

    @Test
    void completelyDifferentTextShouldScoreLow() {
        double score = adapter.similarity("铝合金板5052", "PP塑料粒子");
        assertTrue(score < 30, "完全不同文本相似度应低于 30，实际=" + score);
    }

    @Test
    void similarTextShouldScoreHigh() {
        double score = adapter.similarity("不锈钢板304 2.5*1250*2500", "不锈钢板304 2.5*1220*2440");
        assertTrue(score >= 60, "高度相似文本应不低于 60，实际=" + score);
    }

    @Test
    void normalizedLevenshteinShouldHandlePrefixChange() {
        // 编辑距离 1 / 最大长度 6 → 归一化 5/6
        assertEquals(5.0 / 6.0, LocalSimilarityAdapter.normalizedLevenshtein("ABCDEF", "ABCDEF".replace('A', 'X')),
                1e-9);
    }

    @Test
    void levenshteinShouldComputeClassicDistance() {
        assertEquals(3, LocalSimilarityAdapter.levenshtein("kitten", "sitting"));
        assertEquals(0, LocalSimilarityAdapter.levenshtein("same", "same"));
        assertEquals(4, LocalSimilarityAdapter.levenshtein("", "abcd"));
    }

    @Test
    void jaccard2GramShouldMeasureTokenOverlap() {
        // "abcd" grams={ab,bc,cd}，"abce" grams={ab,bc,ce}，交 2 并 4 = 0.5
        assertEquals(0.5, LocalSimilarityAdapter.jaccard2Gram("abcd", "abce"), 1e-9);
        assertEquals(1.0, LocalSimilarityAdapter.jaccard2Gram("abc", "abc"), 1e-9);
        assertEquals(0.0, LocalSimilarityAdapter.jaccard2Gram("abc", "xyz"), 1e-9);
    }

    @Test
    void scoreShouldBeBoundedIn0To100() {
        assertEquals(0, adapter.similarity("a", "z"));
        assertTrue(adapter.similarity("规格2.5", "规格2.5") <= 100);
    }
}
