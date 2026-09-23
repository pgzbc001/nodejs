package com.mdm.platform.duplicate;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 本地相似度算法（design.md 3.4）：
 * similarity = 0.6 * 编辑距离归一化 + 0.4 * 2-gram Jaccard，输出 0~100。
 */
@Component
public class LocalSimilarityAdapter implements DuplicateCheckAdapter {

    private static final double LEV_WEIGHT = 0.6;
    private static final double JACCARD_WEIGHT = 0.4;

    @Override
    public double similarity(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        if (a.equals(b)) {
            return 100;
        }
        String sa = a.trim();
        String sb = b.trim();
        if (sa.isEmpty() || sb.isEmpty()) {
            return 0;
        }
        double score = LEV_WEIGHT * normalizedLevenshtein(sa, sb) + JACCARD_WEIGHT * jaccard2Gram(sa, sb);
        return Math.round(Math.max(0, Math.min(1, score)) * 100);
    }

    /** 编辑距离归一化相似度：1 - lev / max(len)。 */
    static double normalizedLevenshtein(String a, String b) {
        if (a.equals(b)) {
            return 1;
        }
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1;
        }
        return 1.0 - (double) levenshtein(a, b) / maxLen;
    }

    /** 经典编辑距离 DP（O(lenA*lenB)）。 */
    static int levenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[m];
    }

    /** 2-gram 分词 Jaccard 相似度：|交集| / |并集|。 */
    static double jaccard2Gram(String a, String b) {
        Set<String> ga = bigrams(a);
        Set<String> gb = bigrams(b);
        if (ga.isEmpty() && gb.isEmpty()) {
            return 1;
        }
        if (ga.isEmpty() || gb.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(ga);
        intersection.retainAll(gb);
        Set<String> union = new HashSet<>(ga);
        union.addAll(gb);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> bigrams(String s) {
        Set<String> grams = new HashSet<>();
        for (int i = 0; i + 2 <= s.length(); i++) {
            grams.add(s.substring(i, i + 2));
        }
        return grams;
    }
}
